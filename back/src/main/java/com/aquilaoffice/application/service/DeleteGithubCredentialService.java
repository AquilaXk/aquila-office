package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.DeleteGithubCredentialCommand;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * GitHub 자격 증명 삭제 유스케이스를 수행하는 application service다.
 */
public class DeleteGithubCredentialService implements DeleteGithubCredentialUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadGithubCredentialPort loadGithubCredentialPort;
    private final DeleteGithubCredentialPort deleteGithubCredentialPort;

    public DeleteGithubCredentialService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialPort loadGithubCredentialPort,
            DeleteGithubCredentialPort deleteGithubCredentialPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadGithubCredentialPort =
                Objects.requireNonNull(loadGithubCredentialPort, "loadGithubCredentialPort must not be null");
        this.deleteGithubCredentialPort =
                Objects.requireNonNull(deleteGithubCredentialPort, "deleteGithubCredentialPort must not be null");
    }

    @Override
    public void deleteGithubCredential(DeleteGithubCredentialCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);
        loadGithubCredentialPort
                .findActiveByIdAndWorkspaceId(command.credentialId(), command.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("github credential not found"));

        deleteGithubCredentialPort.delete(command.credentialId(), command.workspaceId());
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
}
