package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.GetArtifactFileQuery;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.in.GetArtifactTreeQuery;
import com.aquilaoffice.application.port.in.GetArtifactTreeUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 artifact 조회 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/artifacts")
public class ArtifactController {

    private final GetArtifactTreeUseCase getArtifactTreeUseCase;
    private final GetArtifactFileUseCase getArtifactFileUseCase;
    private final GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase;
    private final GetOrchestrationPlanFileUseCase getOrchestrationPlanFileUseCase;

    public ArtifactController(
            GetArtifactTreeUseCase getArtifactTreeUseCase,
            GetArtifactFileUseCase getArtifactFileUseCase,
            GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase,
            GetOrchestrationPlanFileUseCase getOrchestrationPlanFileUseCase) {
        this.getArtifactTreeUseCase = getArtifactTreeUseCase;
        this.getArtifactFileUseCase = getArtifactFileUseCase;
        this.getOrchestrationPlanArtifactUseCase = getOrchestrationPlanArtifactUseCase;
        this.getOrchestrationPlanFileUseCase = getOrchestrationPlanFileUseCase;
    }

    @GetMapping("/tree")
    public ResponseEntity<RsData<ArtifactTreeResponse>> getArtifactTree(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(ArtifactTreeResponse.from(getArtifactTreeUseCase.getArtifactTree(
                    new GetArtifactTreeQuery(workspaceId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // artifact 목록도 워크스페이스 멤버에게만 노출하므로 실패 사유를 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/files")
    public ResponseEntity<RsData<ArtifactFileContentResponse>> getArtifactFile(
            @PathVariable Long workspaceId,
            @RequestParam(name = "path", required = false) String path,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        String normalizedPath = requirePath(path);

        try {
            return ResponseEntity.ok(RsData.ok(ArtifactFileContentResponse.from(getArtifactFileUseCase.getArtifactFile(
                    new GetArtifactFileQuery(workspaceId, normalizedPath, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // 존재하지 않는 파일, 권한 실패, 안전하지 않은 경로를 동일하게 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/orchestration-plans/{planId}")
    public ResponseEntity<RsData<OrchestrationPlanArtifactResponse>> getOrchestrationPlanArtifact(
            @PathVariable Long workspaceId,
            @PathVariable Long planId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(OrchestrationPlanArtifactResponse.from(
                    getOrchestrationPlanArtifactUseCase.getOrchestrationPlanArtifact(
                            new GetOrchestrationPlanArtifactQuery(
                                    workspaceId,
                                    planId,
                                    normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // plan artifact도 워크스페이스 멤버에게만 노출하므로 실패 사유를 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/orchestration-plans/{planId}/files")
    public ResponseEntity<RsData<ArtifactFileContentResponse>> getOrchestrationPlanFile(
            @PathVariable Long workspaceId,
            @PathVariable Long planId,
            @RequestParam(name = "path", required = false) String path,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        String normalizedPath = requirePath(path);

        try {
            return ResponseEntity.ok(RsData.ok(ArtifactFileContentResponse.from(
                    getOrchestrationPlanFileUseCase.getOrchestrationPlanFile(new GetOrchestrationPlanFileQuery(
                            workspaceId,
                            planId,
                            normalizedPath,
                            normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // plan file도 존재 여부와 권한 실패, 안전하지 않은 경로를 동일하게 숨긴다.
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

    private String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return path.trim();
    }
}
