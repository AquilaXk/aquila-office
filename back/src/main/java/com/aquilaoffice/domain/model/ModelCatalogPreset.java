package com.aquilaoffice.domain.model;

import java.util.List;

/**
 * 공급자별 기본 엔드포인트와 선별된 추천 모델 프리셋 정보다.
 */
public record ModelCatalogPreset(
        ModelProviderKind providerKind,
        String displayName,
        String defaultBaseUrl,
        ModelWireProtocol wireProtocol,
        ModelProviderTier tier,
        boolean authRequired,
        List<ModelCatalogModel> models) {

    public ModelCatalogPreset {
        models = models == null ? List.of() : List.copyOf(models);
    }
}
