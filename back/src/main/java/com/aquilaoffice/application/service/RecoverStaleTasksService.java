package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.RecoverStaleTasksCommand;
import com.aquilaoffice.application.port.in.RecoverStaleTasksResult;
import com.aquilaoffice.application.port.in.RecoverStaleTasksUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RecoverStaleExecutionsPort;
import com.aquilaoffice.application.port.out.RecoverStaleTasksPort;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 워크스페이스 관리자가 기준 시간(분)을 초과해 고착된 실행과 Task를 FAILED로 복구하는 service다.
 */
public class RecoverStaleTasksService implements RecoverStaleTasksUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final RecoverStaleExecutionsPort recoverStaleExecutionsPort;
    private final RecoverStaleTasksPort recoverStaleTasksPort;
    private final Clock clock;

    public RecoverStaleTasksService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            RecoverStaleExecutionsPort recoverStaleExecutionsPort,
            RecoverStaleTasksPort recoverStaleTasksPort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.recoverStaleExecutionsPort = Objects.requireNonNull(
                recoverStaleExecutionsPort, "recoverStaleExecutionsPort must not be null");
        this.recoverStaleTasksPort =
                Objects.requireNonNull(recoverStaleTasksPort, "recoverStaleTasksPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public RecoverStaleTasksResult recoverStaleTasks(RecoverStaleTasksCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime cutoffUtc = nowUtc.minusMinutes(command.staleMinutes());
        String failureReason = "Recovered stale execution (exceeded " + command.staleMinutes() + " minutes)";

        int recoveredExecutionCount = recoverStaleExecutionsPort.recoverStaleExecutions(
                command.workspaceId(),
                cutoffUtc,
                nowUtc,
                failureReason);
        int recoveredTaskCount = recoverStaleTasksPort.recoverStaleTasks(
                command.workspaceId(),
                cutoffUtc,
                nowUtc);

        return new RecoverStaleTasksResult(recoveredTaskCount, recoveredExecutionCount);
    }

    private void requireAdminMembership(Long workspaceId, String requesterGoogleSub) {
        loadMemberPort.findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        var workspace = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }
}
