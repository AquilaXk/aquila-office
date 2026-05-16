package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusResult;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.gateway.GatewayBindingStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import java.util.Objects;

/**
 * 워크스페이스 gateway binding 상태 조회 유스케이스를 수행하는 application service다.
 */
public class GetWorkspaceGatewayStatusService implements GetWorkspaceGatewayStatusUseCase {

    private static final String EXTERNAL_MODE = "EXTERNAL";

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;

    public GetWorkspaceGatewayStatusService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceGatewayBindingPort =
                Objects.requireNonNull(loadWorkspaceGatewayBindingPort, "loadWorkspaceGatewayBindingPort must not be null");
    }

    @Override
    public GetWorkspaceGatewayStatusResult getWorkspaceGatewayStatus(GetWorkspaceGatewayStatusQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Member requester = loadRequester(query.requesterGoogleSub());
        loadWorkspaceDetail(query.workspaceId(), requester.getGoogleSub());

        return loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(query.workspaceId())
                .map(this::toBoundResult)
                .orElseGet(this::toUnboundResult);
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

    private GetWorkspaceGatewayStatusResult toBoundResult(WorkspaceGatewayBinding binding) {
        return new GetWorkspaceGatewayStatusResult(
                GatewayBindingStatus.BOUND,
                true,
                binding.getId(),
                EXTERNAL_MODE,
                binding.getGatewayUrl(),
                binding.maskedToken(),
                null,
                null,
                null,
                null);
    }

    private GetWorkspaceGatewayStatusResult toUnboundResult() {
        return new GetWorkspaceGatewayStatusResult(
                GatewayBindingStatus.UNBOUND,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
