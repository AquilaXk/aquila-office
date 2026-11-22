package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.model.ModelCatalogPreset;
import java.util.List;

/**
 * 추천 AI 모델 카탈로그 프리셋 목록을 불러온다.
 */
public interface LoadModelCatalogPort {

    List<ModelCatalogPreset> loadPresets();
}
