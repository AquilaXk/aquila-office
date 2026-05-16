package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.GetArtifactFileQuery;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.in.GetArtifactTreeQuery;
import com.aquilaoffice.application.port.in.GetArtifactTreeUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
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

    public ArtifactController(
            GetArtifactTreeUseCase getArtifactTreeUseCase,
            GetArtifactFileUseCase getArtifactFileUseCase,
            GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase) {
        this.getArtifactTreeUseCase = getArtifactTreeUseCase;
        this.getArtifactFileUseCase = getArtifactFileUseCase;
        this.getOrchestrationPlanArtifactUseCase = getOrchestrationPlanArtifactUseCase;
    }

    @GetMapping("/tree")
    public ResponseEntity<RsData<ArtifactTreeResponse>> getArtifactTree(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);
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
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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

    private String requireRequesterGoogleSub(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return requesterGoogleSub.trim();
    }

    private String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return path.trim();
    }
}
