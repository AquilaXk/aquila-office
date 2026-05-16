package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionCommand;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionResult;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * gateway 연결 테스트 유스케이스를 수행하는 application service다.
 */
public class TestWorkspaceGatewayConnectionService implements TestWorkspaceGatewayConnectionUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;

    public TestWorkspaceGatewayConnectionService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
    }

    @Override
    public TestWorkspaceGatewayConnectionResult testWorkspaceGatewayConnection(
            TestWorkspaceGatewayConnectionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        // 실제 gateway 호출은 별도 integration PR에서 다루고, 현재 단계는 저장 전 입력 검증 계약만 고정한다.
        return new TestWorkspaceGatewayConnectionResult(
                GatewayConnectionStatus.CONNECTED,
                true,
                command.gatewayUrl(),
                "gateway connection accepted",
                0);
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
