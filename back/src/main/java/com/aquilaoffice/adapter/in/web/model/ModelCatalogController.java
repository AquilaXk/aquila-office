package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.GetModelCatalogUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 선별된 AI 모델 공급자 및 추천 모델 카탈로그 조회 엔드포인트다.
 */
@RestController
@RequestMapping("/api/v1/model-catalog")
@Tag(name = "Model Catalog", description = "AI 모델 공급자 및 추천 모델 카탈로그 API")
@SecurityRequirement(name = "bearerAuth")
public class ModelCatalogController {

    private final GetModelCatalogUseCase getModelCatalogUseCase;

    public ModelCatalogController(GetModelCatalogUseCase getModelCatalogUseCase) {
        this.getModelCatalogUseCase = Objects.requireNonNull(
                getModelCatalogUseCase, "getModelCatalogUseCase must not be null");
    }

    @GetMapping
    @Operation(summary = "지원 중인 AI 모델 공급자 프리셋 카탈로그 조회")
    public ResponseEntity<RsData<ModelCatalogResponse>> getModelCatalog(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(RsData.ok(ModelCatalogResponse.from(getModelCatalogUseCase.getCatalog())));
    }
}
