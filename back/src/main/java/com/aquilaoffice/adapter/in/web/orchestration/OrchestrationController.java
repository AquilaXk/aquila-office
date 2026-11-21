package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsQuery;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Multi-Agent 실행 계획 세션 생성, 조회, 디스패치를 처리하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/orchestrations")
@Tag(name = "Orchestration", description = "Multi-Agent 실행 계획 생성, 조회, 디스패치 API")
@SecurityRequirement(name = "bearerAuth")
public class OrchestrationController {

    private final CreateOrchestrationSessionUseCase createOrchestrationSessionUseCase;
    private final ListOrchestrationSessionsUseCase listOrchestrationSessionsUseCase;
    private final GetOrchestrationSessionUseCase getOrchestrationSessionUseCase;
    private final DispatchOrchestrationSessionUseCase dispatchOrchestrationSessionUseCase;

    public OrchestrationController(
            CreateOrchestrationSessionUseCase createOrchestrationSessionUseCase,
            ListOrchestrationSessionsUseCase listOrchestrationSessionsUseCase,
            GetOrchestrationSessionUseCase getOrchestrationSessionUseCase,
            DispatchOrchestrationSessionUseCase dispatchOrchestrationSessionUseCase) {
        this.createOrchestrationSessionUseCase = createOrchestrationSessionUseCase;
        this.listOrchestrationSessionsUseCase = listOrchestrationSessionsUseCase;
        this.getOrchestrationSessionUseCase = getOrchestrationSessionUseCase;
        this.dispatchOrchestrationSessionUseCase = dispatchOrchestrationSessionUseCase;
    }

    @PostMapping
    @Operation(summary = "Multi-Agent 실행 계획 세션을 생성한다")
    public ResponseEntity<RsData<OrchestrationSessionResponse>> createSession(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateOrchestrationSessionRequest request) {
        String requesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        if (workspaceId == null || workspaceId <= 0) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        try {
            CreateOrchestrationSessionCommand command = request.toCommand(workspaceId, requesterGoogleSub);
            CreateOrchestrationSessionResult result = createOrchestrationSessionUseCase.createSession(command);
            URI location = URI.create(
                    "/api/v1/workspaces/" + workspaceId + "/orchestrations/" + result.sessionId());
            return ResponseEntity.created(location).body(RsData.ok(OrchestrationSessionResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            if (isValidationMessage(exception.getMessage())) {
                throw new ServiceException(CommonErrorCode.BAD_REQUEST);
            }
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping
    @Operation(summary = "워크스페이스 Multi-Agent 실행 계획 목록을 조회한다")
    public ResponseEntity<RsData<List<OrchestrationSessionResponse>>> listSessions(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String requesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        ListOrchestrationSessionsQuery query;
        try {
            query = new ListOrchestrationSessionsQuery(workspaceId, requesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(listOrchestrationSessionsUseCase.listSessions(query)
                    .stream()
                    .map(OrchestrationSessionResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "단건 Multi-Agent 실행 계획 상세를 조회한다")
    public ResponseEntity<RsData<OrchestrationSessionResponse>> getSession(
            @PathVariable Long workspaceId,
            @PathVariable Long sessionId,
            Authentication authentication) {
        String requesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        GetOrchestrationSessionQuery query;
        try {
            query = new GetOrchestrationSessionQuery(workspaceId, sessionId, requesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(
                    OrchestrationSessionResponse.from(getOrchestrationSessionUseCase.getSession(query))));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{sessionId}/dispatch")
    @Operation(summary = "선행 조건이 충족된 실행 계획 step을 Task로 디스패치한다")
    public ResponseEntity<RsData<DispatchOrchestrationSessionResponse>> dispatchSession(
            @PathVariable Long workspaceId,
            @PathVariable Long sessionId,
            Authentication authentication,
            @RequestBody(required = false) DispatchOrchestrationSessionRequest request) {
        String requesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        if (workspaceId == null || workspaceId <= 0 || sessionId == null || sessionId <= 0) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        DispatchOrchestrationSessionRequest safeRequest =
                request != null ? request : new DispatchOrchestrationSessionRequest(false);
        DispatchOrchestrationSessionCommand command =
                safeRequest.toCommand(workspaceId, sessionId, requesterGoogleSub);

        try {
            return ResponseEntity.ok(RsData.ok(DispatchOrchestrationSessionResponse.from(
                    dispatchOrchestrationSessionUseCase.dispatchSession(command))));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private static boolean isValidationMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.startsWith("orchestration step")
                || message.startsWith("stepKey")
                || message.startsWith("instruction")
                || message.startsWith("dependsOn");
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
