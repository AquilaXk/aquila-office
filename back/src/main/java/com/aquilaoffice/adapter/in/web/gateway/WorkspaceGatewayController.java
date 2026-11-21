package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusResult;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionResult;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스별 gateway HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/gateway")
@Tag(name = "Gateway", description = "워크스페이스 gateway 연결 상태와 binding API")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceGatewayController {

    private final GetWorkspaceGatewayStatusUseCase getWorkspaceGatewayStatusUseCase;
    private final TestWorkspaceGatewayConnectionUseCase testWorkspaceGatewayConnectionUseCase;
    private final CreateWorkspaceGatewayBindingUseCase createWorkspaceGatewayBindingUseCase;

    public WorkspaceGatewayController(
            GetWorkspaceGatewayStatusUseCase getWorkspaceGatewayStatusUseCase,
            TestWorkspaceGatewayConnectionUseCase testWorkspaceGatewayConnectionUseCase,
            CreateWorkspaceGatewayBindingUseCase createWorkspaceGatewayBindingUseCase) {
        this.getWorkspaceGatewayStatusUseCase = getWorkspaceGatewayStatusUseCase;
        this.testWorkspaceGatewayConnectionUseCase = testWorkspaceGatewayConnectionUseCase;
        this.createWorkspaceGatewayBindingUseCase = createWorkspaceGatewayBindingUseCase;
    }

    @GetMapping
    @Operation(summary = "워크스페이스 gateway 연결 상태를 조회한다")
    public ResponseEntity<RsData<WorkspaceGatewayStatusResponse>> getWorkspaceGatewayStatus(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            GetWorkspaceGatewayStatusResult result = getWorkspaceGatewayStatusUseCase.getWorkspaceGatewayStatus(
                    new GetWorkspaceGatewayStatusQuery(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceGatewayStatusResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // gateway 상태는 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/connection-test")
    public ResponseEntity<RsData<WorkspaceGatewayConnectionTestResponse>> testWorkspaceGatewayConnection(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody TestWorkspaceGatewayConnectionRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            TestWorkspaceGatewayConnectionResult result =
                    testWorkspaceGatewayConnectionUseCase.testWorkspaceGatewayConnection(
                            request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceGatewayConnectionTestResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 연결 테스트는 관리자에게만 허용하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/binding")
    public ResponseEntity<RsData<WorkspaceGatewayBindingResponse>> createWorkspaceGatewayBinding(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceGatewayBindingRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            CreateWorkspaceGatewayBindingResult result =
                    createWorkspaceGatewayBindingUseCase.createWorkspaceGatewayBinding(
                            request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.created(URI.create("/api/v1/workspaces/"
                            + result.workspaceId()
                            + "/gateway/binding"))
                    .body(RsData.ok(WorkspaceGatewayBindingResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 등록 권한이 없거나 대상 워크스페이스를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
