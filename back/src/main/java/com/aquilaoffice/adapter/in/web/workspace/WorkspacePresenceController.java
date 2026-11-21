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
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    public WorkspacePresenceController(
            UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase,
            OpenWorkspacePresenceStreamUseCase openWorkspacePresenceStreamUseCase,
            CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase) {
        this.updateWorkspacePresencePositionUseCase = updateWorkspacePresencePositionUseCase;
        this.openWorkspacePresenceStreamUseCase = openWorkspacePresenceStreamUseCase;
        this.closeWorkspacePresenceStreamUseCase = closeWorkspacePresenceStreamUseCase;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String accessToken) {
        String resolvedMemberGoogleSub =
                resolveMemberGoogleSub(authentication, authorizationHeader, token, accessToken);
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

    private String resolveMemberGoogleSub(
            Authentication authentication,
            String authorizationHeader,
            String token,
            String accessToken) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember) {
            return authenticatedMember.googleSub();
        }
        String bearerToken = resolveBearerToken(authorizationHeader);
        if (bearerToken != null) {
            return bearerToken;
        }
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken.trim();
        }
        throw new ServiceException(CommonErrorCode.BAD_REQUEST);
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String trimmed = authorizationHeader.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String bearerValue = trimmed.substring("bearer ".length()).trim();
            return bearerValue.isBlank() ? null : bearerValue;
        }
        return trimmed;
    }
}
