package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * GitHub 자격 증명 수정 유스케이스를 수행하는 application service다.
 */
public class UpdateGithubCredentialService implements UpdateGithubCredentialUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadGithubCredentialPort loadGithubCredentialPort;
    private final UpdateGithubCredentialPort updateGithubCredentialPort;

    public UpdateGithubCredentialService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialPort loadGithubCredentialPort,
            UpdateGithubCredentialPort updateGithubCredentialPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadGithubCredentialPort =
                Objects.requireNonNull(loadGithubCredentialPort, "loadGithubCredentialPort must not be null");
        this.updateGithubCredentialPort =
                Objects.requireNonNull(updateGithubCredentialPort, "updateGithubCredentialPort must not be null");
    }

    @Override
    public UpdateGithubCredentialResult updateGithubCredential(UpdateGithubCredentialCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        GithubCredential credential = loadGithubCredentialPort
                .findActiveByIdAndWorkspaceId(command.credentialId(), command.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("github credential not found"));
        credential.update(command.displayName(), command.token());

        return toResult(updateGithubCredentialPort.update(credential));
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

    private UpdateGithubCredentialResult toResult(GithubCredential credential) {
        return new UpdateGithubCredentialResult(
                credential.getId(),
                credential.getWorkspaceId(),
                credential.getDisplayName(),
                credential.maskedToken());
    }
}
