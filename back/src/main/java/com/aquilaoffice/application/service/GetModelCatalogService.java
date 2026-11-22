package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetModelCatalogUseCase;
import com.aquilaoffice.application.port.out.LoadModelCatalogPort;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import java.util.List;
import java.util.Objects;

/**
 * 추천 AI 모델 카탈로그 프리셋 목록을 조회하는 application service다.
 */
public class GetModelCatalogService implements GetModelCatalogUseCase {

    private final LoadModelCatalogPort loadModelCatalogPort;

    public GetModelCatalogService(LoadModelCatalogPort loadModelCatalogPort) {
        this.loadModelCatalogPort = Objects.requireNonNull(
                loadModelCatalogPort, "loadModelCatalogPort must not be null");
    }

    @Override
    public List<ModelCatalogPreset> getCatalog() {
        return loadModelCatalogPort.loadPresets();
    }
}
