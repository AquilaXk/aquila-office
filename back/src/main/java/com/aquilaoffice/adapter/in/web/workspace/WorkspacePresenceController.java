package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamResult;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import com.aquilaoffice.global.security.PresenceStreamTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 워크스페이스 presence 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/presence")
public class WorkspacePresenceController {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase;
    private final OpenWorkspacePresenceStreamUseCase openWorkspacePresenceStreamUseCase;
    private final CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase;
    private final PresenceStreamTicketService presenceStreamTicketService;

    @Autowired
    public WorkspacePresenceController(
            UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase,
            OpenWorkspacePresenceStreamUseCase openWorkspacePresenceStreamUseCase,
            CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase,
            PresenceStreamTicketService presenceStreamTicketService) {
        this.updateWorkspacePresencePositionUseCase = updateWorkspacePresencePositionUseCase;
        this.openWorkspacePresenceStreamUseCase = openWorkspacePresenceStreamUseCase;
        this.closeWorkspacePresenceStreamUseCase = closeWorkspacePresenceStreamUseCase;
        this.presenceStreamTicketService = presenceStreamTicketService;
    }

    @PostMapping("/stream-ticket")
    @Operation(summary = "presence SSE 연결용 일회성 티켓 발급")
    public ResponseEntity<RsData<PresenceStreamTicketResponse>> issueStreamTicket(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String memberGoogleSub = requireAuthenticatedMemberGoogleSub(authentication);
        PresenceStreamTicketService.IssuedTicket issued;
        try {
            issued = presenceStreamTicketService.issueTicket(workspaceId, memberGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return ResponseEntity.ok(RsData.ok(
                new PresenceStreamTicketResponse(issued.ticket(), issued.expiresInSeconds())));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "워크스페이스 presence SSE 스트림 연결")
    public SseEmitter stream(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String resolvedMemberGoogleSub = requireAuthenticatedMemberGoogleSub(authentication);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        SseWorkspacePresenceEventSink sink = new SseWorkspacePresenceEventSink(emitter);

        OpenWorkspacePresenceStreamResult streamResult;
        try {
            streamResult = openWorkspacePresenceStreamUseCase.openStream(
                    new OpenWorkspacePresenceStreamCommand(workspaceId, resolvedMemberGoogleSub),
                    sink);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }

        Runnable disconnect = () -> {
            try {
                closeWorkspacePresenceStreamUseCase.closeStream(new CloseWorkspacePresenceStreamCommand(
                        streamResult.workspaceId(),
                        streamResult.memberId(),
                        sink));
            } catch (RuntimeException ignored) {
                // SSE 종료 callback은 응답 이후 실행되므로 cleanup 실패를 클라이언트 응답으로 되돌릴 수 없다.
            }
        };
        emitter.onCompletion(disconnect);
        emitter.onTimeout(disconnect);
        emitter.onError(error -> disconnect.run());

        return emitter;
    }

    @PatchMapping("/me/position")
    @Operation(summary = "현재 사용자의 워크스페이스 오피스 위치 갱신")
    public ResponseEntity<RsData<Void>> updateMyPosition(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody(required = false) WorkspacePresencePositionUpdateRequest request) {
        String normalizedMemberGoogleSub = requireAuthenticatedMemberGoogleSub(authentication);
        if (request == null) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            updateWorkspacePresencePositionUseCase.updatePosition(
                    request.toCommand(workspaceId, normalizedMemberGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // presence 상태는 가입된 워크스페이스 멤버에게만 노출하므로 미가입/미존재를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireAuthenticatedMemberGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }

    @Schema(description = "SSE presence 연결용 일회성 티켓 응답")
    public record PresenceStreamTicketResponse(
            @Schema(description = "일회성 SSE 연결 티켓") String ticket,
            @Schema(description = "만료 시간(초)") int expiresInSeconds) {}
}
