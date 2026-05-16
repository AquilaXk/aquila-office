package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamResult;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import java.util.List;
import java.util.Objects;

/**
 * presence stream 연결을 등록하고 워크스페이스 접속 이벤트를 broadcast하는 application service다.
 */
public class OpenWorkspacePresenceStreamService
        implements OpenWorkspacePresenceStreamUseCase, CloseWorkspacePresenceStreamUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final WorkspacePresenceRegistry workspacePresenceRegistry;

    public OpenWorkspacePresenceStreamService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            WorkspacePresenceRegistry workspacePresenceRegistry) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
        this.workspacePresenceRegistry =
                Objects.requireNonNull(workspacePresenceRegistry, "workspacePresenceRegistry must not be null");
    }

    @Override
    public OpenWorkspacePresenceStreamResult openStream(
            OpenWorkspacePresenceStreamCommand command,
            WorkspacePresenceEventSink sink) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(sink, "sink must not be null");

        Long memberId = resolveMemberId(command.workspaceId(), command.memberGoogleSub());
        WorkspacePresenceRegistry.JoinResult result =
                workspacePresenceRegistry.join(command.workspaceId(), memberId, sink);
        try {
            sink.send(result.snapshot());
            if (result.joinEvent() != null) {
                broadcast(command.workspaceId(), result.joinEvent(), List.of(sink));
            }
        } catch (RuntimeException exception) {
            closeStream(new CloseWorkspacePresenceStreamCommand(command.workspaceId(), memberId, sink));
            throw exception;
        }

        return new OpenWorkspacePresenceStreamResult(command.workspaceId(), memberId);
    }

    @Override
    public void closeStream(CloseWorkspacePresenceStreamCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspacePresenceRegistry.LeaveResult result =
                workspacePresenceRegistry.leave(command.workspaceId(), command.memberId(), command.sink());
        if (result.leaveEvent() != null) {
            broadcast(command.workspaceId(), result.leaveEvent(), List.of(command.sink()));
        }
    }

    private Long resolveMemberId(Long workspaceId, String memberGoogleSub) {
        Member requester = loadMemberPort
                .findByGoogleSub(memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));

        return loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(workspaceId).stream()
                .filter(member -> member.memberGoogleSub().equals(requester.getGoogleSub()))
                .map(ListWorkspaceMembersResult::memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("workspace member not found"));
    }

    private void broadcast(
            Long workspaceId,
            WorkspacePresenceMessage message,
            List<WorkspacePresenceEventSink> excludedSinks) {
        for (WorkspacePresenceRegistry.WorkspacePresenceConnection connection :
                workspacePresenceRegistry.connectionsInWorkspace(workspaceId)) {
            if (excludedSinks.contains(connection.sink())) {
                continue;
            }
            try {
                connection.sink().send(message);
            } catch (RuntimeException exception) {
                closeStream(new CloseWorkspacePresenceStreamCommand(
                        connection.workspaceId(),
                        connection.memberId(),
                        connection.sink()));
            }
        }
    }
}
