package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelProvidersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelTelemetryUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.WorkspaceModelProviderResult;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 AI 모델 공급자 관리 및 연결 테스트 API 컨트롤러다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/model-providers")
@Tag(name = "Model Provider", description = "워크스페이스 AI 모델 공급자 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceModelProviderController {

    private final ListWorkspaceModelProvidersUseCase listWorkspaceModelProvidersUseCase;
    private final ListWorkspaceModelTelemetryUseCase listWorkspaceModelTelemetryUseCase;
    private final CreateWorkspaceModelProviderUseCase createWorkspaceModelProviderUseCase;
    private final UpdateWorkspaceModelProviderUseCase updateWorkspaceModelProviderUseCase;
    private final DeleteWorkspaceModelProviderUseCase deleteWorkspaceModelProviderUseCase;
    private final TestWorkspaceModelProviderUseCase testWorkspaceModelProviderUseCase;

    public WorkspaceModelProviderController(
            ListWorkspaceModelProvidersUseCase listWorkspaceModelProvidersUseCase,
            CreateWorkspaceModelProviderUseCase createWorkspaceModelProviderUseCase,
            UpdateWorkspaceModelProviderUseCase updateWorkspaceModelProviderUseCase,
            DeleteWorkspaceModelProviderUseCase deleteWorkspaceModelProviderUseCase,
            TestWorkspaceModelProviderUseCase testWorkspaceModelProviderUseCase) {
        this(
                listWorkspaceModelProvidersUseCase,
                (workspaceId, googleSub) -> List.of(),
                createWorkspaceModelProviderUseCase,
                updateWorkspaceModelProviderUseCase,
                deleteWorkspaceModelProviderUseCase,
                testWorkspaceModelProviderUseCase);
    }

    @Autowired
    public WorkspaceModelProviderController(
            ListWorkspaceModelProvidersUseCase listWorkspaceModelProvidersUseCase,
            ListWorkspaceModelTelemetryUseCase listWorkspaceModelTelemetryUseCase,
            CreateWorkspaceModelProviderUseCase createWorkspaceModelProviderUseCase,
            UpdateWorkspaceModelProviderUseCase updateWorkspaceModelProviderUseCase,
            DeleteWorkspaceModelProviderUseCase deleteWorkspaceModelProviderUseCase,
            TestWorkspaceModelProviderUseCase testWorkspaceModelProviderUseCase) {
        this.listWorkspaceModelProvidersUseCase = listWorkspaceModelProvidersUseCase;
        this.listWorkspaceModelTelemetryUseCase = listWorkspaceModelTelemetryUseCase;
        this.createWorkspaceModelProviderUseCase = createWorkspaceModelProviderUseCase;
        this.updateWorkspaceModelProviderUseCase = updateWorkspaceModelProviderUseCase;
        this.deleteWorkspaceModelProviderUseCase = deleteWorkspaceModelProviderUseCase;
        this.testWorkspaceModelProviderUseCase = testWorkspaceModelProviderUseCase;
    }

    @GetMapping
    @Operation(summary = "워크스페이스에 등록된 AI 모델 공급자 목록 조회")
    public ResponseEntity<RsData<List<WorkspaceModelProviderResponse>>> list(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            List<WorkspaceModelProviderResponse> providers = listWorkspaceModelProvidersUseCase
                    .list(workspaceId, googleSub)
                    .stream()
                    .map(WorkspaceModelProviderResponse::from)
                    .toList();
            return ResponseEntity.ok(RsData.ok(providers));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    @GetMapping("/telemetry")
    @Operation(summary = "워크스페이스의 모델별 실행 통계 조회")
    public ResponseEntity<RsData<List<WorkspaceModelTelemetryResponse>>> telemetry(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            List<WorkspaceModelTelemetryResponse> entries = listWorkspaceModelTelemetryUseCase
                    .listTelemetry(workspaceId, googleSub)
                    .stream()
                    .map(WorkspaceModelTelemetryResponse::from)
                    .toList();
            return ResponseEntity.ok(RsData.ok(entries));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    @PostMapping
    @Operation(summary = "워크스페이스 AI 모델 공급자 등록")
    public ResponseEntity<RsData<WorkspaceModelProviderResponse>> create(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceModelProviderRequest request) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            WorkspaceModelProviderResult result = createWorkspaceModelProviderUseCase.create(
                    request.toCommand(workspaceId, googleSub));
            return ResponseEntity.created(URI.create(
                            "/api/v1/workspaces/" + workspaceId + "/model-providers/" + result.providerId()))
                    .body(RsData.ok(WorkspaceModelProviderResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    @PatchMapping("/{providerId}")
    @Operation(summary = "워크스페이스 AI 모델 공급자 수정")
    public ResponseEntity<RsData<WorkspaceModelProviderResponse>> update(
            @PathVariable Long workspaceId,
            @PathVariable Long providerId,
            Authentication authentication,
            @Valid @RequestBody UpdateWorkspaceModelProviderRequest request) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            WorkspaceModelProviderResult result = updateWorkspaceModelProviderUseCase.update(
                    request.toCommand(workspaceId, providerId, googleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceModelProviderResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    @DeleteMapping("/{providerId}")
    @Operation(summary = "워크스페이스 AI 모델 공급자 삭제")
    public ResponseEntity<RsData<Void>> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long providerId,
            Authentication authentication) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            deleteWorkspaceModelProviderUseCase.delete(workspaceId, providerId, googleSub);
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    @PostMapping("/connection-test")
    @Operation(summary = "워크스페이스 AI 모델 공급자 연결 테스트")
    public ResponseEntity<RsData<WorkspaceModelConnectionTestResponse>> testConnection(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody TestWorkspaceModelProviderRequest request) {
        String googleSub = requireAuthenticatedGoogleSub(authentication);
        try {
            var result = testWorkspaceModelProviderUseCase.testConnection(
                    request.toCommand(workspaceId, googleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceModelConnectionTestResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            throw mapArgumentException(exception);
        }
    }

    private static String requireAuthenticatedGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }

    private static ServiceException mapArgumentException(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("membership") || message.contains("not found")) {
            return new ServiceException(CommonErrorCode.NOT_FOUND);
        }
        return new ServiceException(CommonErrorCode.BAD_REQUEST);
    }
}
