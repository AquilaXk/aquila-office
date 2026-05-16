package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsQuery;
import com.aquilaoffice.application.port.in.ListGithubCredentialsResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import java.util.List;
import java.util.Objects;

/**
 * GitHub 자격 증명 목록 조회 유스케이스를 수행하는 application service다.
 */
public class ListGithubCredentialsService implements ListGithubCredentialsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadGithubCredentialsPort loadGithubCredentialsPort;

    public ListGithubCredentialsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialsPort loadGithubCredentialsPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadGithubCredentialsPort =
                Objects.requireNonNull(loadGithubCredentialsPort, "loadGithubCredentialsPort must not be null");
    }

    @Override
    public List<ListGithubCredentialsResult> listGithubCredentials(ListGithubCredentialsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(query.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        // 목록은 워크스페이스 멤버 이상이면 볼 수 있으므로 상세 조회 성공 자체를 멤버십 검증으로 사용한다.
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));

        return loadGithubCredentialsPort.findActiveByWorkspaceId(query.workspaceId()).stream()
                .map(this::toResult)
                .toList();
    }

    private ListGithubCredentialsResult toResult(GithubCredential credential) {
        return new ListGithubCredentialsResult(
                credential.getId(),
                credential.getWorkspaceId(),
                credential.getDisplayName(),
                credential.maskedToken());
    }
}
