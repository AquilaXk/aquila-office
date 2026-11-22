package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelCatalogPreset;
import java.util.List;

/**
 * 추천 AI 모델 카탈로그 프리셋 조회 use case다.
 */
public interface GetModelCatalogUseCase {

    List<ModelCatalogPreset> getCatalog();
}
