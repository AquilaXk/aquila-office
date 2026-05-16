package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.CreateGithubCredentialResult;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * GitHub 자격 증명 등록 유스케이스를 수행하는 application service다.
 */
public class CreateGithubCredentialService implements CreateGithubCredentialUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final ExistsGithubCredentialPort existsGithubCredentialPort;
    private final SaveGithubCredentialPort saveGithubCredentialPort;

    public CreateGithubCredentialService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            ExistsGithubCredentialPort existsGithubCredentialPort,
            SaveGithubCredentialPort saveGithubCredentialPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.existsGithubCredentialPort =
                Objects.requireNonNull(existsGithubCredentialPort, "existsGithubCredentialPort must not be null");
        this.saveGithubCredentialPort =
                Objects.requireNonNull(saveGithubCredentialPort, "saveGithubCredentialPort must not be null");
    }

    @Override
    public CreateGithubCredentialResult createGithubCredential(CreateGithubCredentialCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        if (existsGithubCredentialPort.existsActiveDisplayName(command.workspaceId(), command.displayName())) {
            throw new IllegalStateException("github credential displayName already exists");
        }

        GithubCredential credential = GithubCredential.create(
                command.workspaceId(), command.displayName(), command.token(), requester.getGoogleSub());
        return toResult(saveGithubCredentialPort.save(credential));
    }

    private Member loadRequester(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
    }

    private GetWorkspaceDetailResult loadWorkspaceDetail(Long workspaceId, String googleSub) {
        return loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
    }

    private void requireAdmin(GetWorkspaceDetailResult workspaceDetail) {
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private CreateGithubCredentialResult toResult(GithubCredential credential) {
        return new CreateGithubCredentialResult(
                credential.getId(),
                credential.getWorkspaceId(),
                credential.getDisplayName(),
                credential.maskedToken());
    }
}
