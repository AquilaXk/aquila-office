package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.domain.model.ModelCatalogModel;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelProviderTier;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 추천 AI 모델 카탈로그 HTTP 응답이다.
 */
@Schema(description = "AI 모델 카탈로그 프리셋 목록 응답")
public record ModelCatalogResponse(List<PresetResponse> presets) {

    public static ModelCatalogResponse from(List<ModelCatalogPreset> presets) {
        return new ModelCatalogResponse(presets.stream().map(PresetResponse::from).toList());
    }

    @Schema(description = "AI 모델 공급자 프리셋")
    public record PresetResponse(
            ModelProviderKind providerKind,
            String displayName,
            String defaultBaseUrl,
            ModelWireProtocol wireProtocol,
            ModelProviderTier tier,
            boolean authRequired,
            List<ModelResponse> models) {

        public static PresetResponse from(ModelCatalogPreset preset) {
            return new PresetResponse(
                    preset.providerKind(),
                    preset.displayName(),
                    preset.defaultBaseUrl(),
                    preset.wireProtocol(),
                    preset.tier(),
                    preset.authRequired(),
                    preset.models().stream().map(ModelResponse::from).toList());
        }
    }

    @Schema(description = "카탈로그 개별 모델 스펙")
    public record ModelResponse(
            String id,
            String displayName,
            int contextWindow,
            int maxOutputTokens,
            boolean supportsReasoning,
            boolean supportsTools,
            boolean supportsVision) {

        public static ModelResponse from(ModelCatalogModel model) {
            return new ModelResponse(
                    model.id(),
                    model.displayName(),
                    model.contextWindow(),
                    model.maxOutputTokens(),
                    model.supportsReasoning(),
                    model.supportsTools(),
                    model.supportsVision());
        }
    }
}
