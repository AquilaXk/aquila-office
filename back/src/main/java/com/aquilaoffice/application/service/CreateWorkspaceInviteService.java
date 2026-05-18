package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RecordWorkspaceInviteEmailFailurePort;
import com.aquilaoffice.application.port.out.RunAfterCommitPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceInvite;
import com.aquilaoffice.application.port.out.SendWorkspaceInviteEmailPort;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailCommand;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailFailure;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 초대 생성 유스케이스를 수행하는 application service다.
 */
public class CreateWorkspaceInviteService implements CreateWorkspaceInviteUseCase {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final SaveWorkspaceInvitePort saveWorkspaceInvitePort;
    private final GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort;
    private final Clock clock;
    private final RunAfterCommitPort runAfterCommitPort;
    private final SendWorkspaceInviteEmailPort sendWorkspaceInviteEmailPort;
    private final RecordWorkspaceInviteEmailFailurePort recordWorkspaceInviteEmailFailurePort;

    public CreateWorkspaceInviteService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceInvitePort saveWorkspaceInvitePort,
            GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort,
            Clock clock) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                saveWorkspaceInvitePort,
                generateWorkspaceInviteTokenPort,
                clock,
                RunAfterCommitPort.immediate(),
                SendWorkspaceInviteEmailPort.noop(),
                RecordWorkspaceInviteEmailFailurePort.noop());
    }

    public CreateWorkspaceInviteService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceInvitePort saveWorkspaceInvitePort,
            GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort,
            Clock clock,
            RunAfterCommitPort runAfterCommitPort,
            SendWorkspaceInviteEmailPort sendWorkspaceInviteEmailPort,
            RecordWorkspaceInviteEmailFailurePort recordWorkspaceInviteEmailFailurePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.saveWorkspaceInvitePort =
                Objects.requireNonNull(saveWorkspaceInvitePort, "saveWorkspaceInvitePort must not be null");
        this.generateWorkspaceInviteTokenPort = Objects.requireNonNull(
                generateWorkspaceInviteTokenPort, "generateWorkspaceInviteTokenPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.runAfterCommitPort = Objects.requireNonNull(runAfterCommitPort, "runAfterCommitPort must not be null");
        this.sendWorkspaceInviteEmailPort =
                Objects.requireNonNull(sendWorkspaceInviteEmailPort, "sendWorkspaceInviteEmailPort must not be null");
        this.recordWorkspaceInviteEmailFailurePort = Objects.requireNonNull(
                recordWorkspaceInviteEmailFailurePort, "recordWorkspaceInviteEmailFailurePort must not be null");
    }

    @Override
    public CreateWorkspaceInviteResult createWorkspaceInvite(CreateWorkspaceInviteCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(command.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        GetWorkspaceDetailResult workspaceDetail = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(command.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }

        LocalDateTime createdAt = LocalDateTime.now(clock);
        Workspace workspace = Workspace.create(workspaceDetail.name(), workspaceDetail.description(), requester);
        // 실제 FK는 workspaceId로 저장하고, domain 객체는 초대 값 검증과 상태 초기화에 사용한다.
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace,
                command.email(),
                generateWorkspaceInviteTokenPort.generateToken(),
                createdAt,
                createdAt.plus(INVITE_TTL));
        SavedWorkspaceInvite savedInvite = saveWorkspaceInvitePort.save(workspaceDetail.workspaceId(), invite);
        scheduleInviteEmail(savedInvite, workspaceDetail, invite);

        return new CreateWorkspaceInviteResult(
                savedInvite.inviteId(),
                workspaceDetail.workspaceId(),
                invite.getEmail(),
                invite.getToken(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt());
    }

    private void scheduleInviteEmail(
            SavedWorkspaceInvite savedInvite,
            GetWorkspaceDetailResult workspaceDetail,
            WorkspaceInvite invite) {
        WorkspaceInviteEmailCommand emailCommand = new WorkspaceInviteEmailCommand(
                savedInvite.inviteId(),
                workspaceDetail.workspaceId(),
                workspaceDetail.name(),
                invite.getEmail(),
                invite.getToken(),
                invite.getExpiresAt());
        runAfterCommitPort.runAfterCommit(() -> sendInviteEmail(emailCommand));
    }

    private void sendInviteEmail(WorkspaceInviteEmailCommand emailCommand) {
        try {
            sendWorkspaceInviteEmailPort.send(emailCommand);
        } catch (RuntimeException exception) {
            recordWorkspaceInviteEmailFailurePort.record(new WorkspaceInviteEmailFailure(
                    emailCommand.inviteId(),
                    emailCommand.workspaceId(),
                    resolveFailureReason(exception),
                    LocalDateTime.now(clock)));
        }
    }

    private static String resolveFailureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
