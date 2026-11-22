package com.aquilaoffice.domain.model;

/**
 * 카탈로그 프리셋 내부 개별 모델의 스펙 메타데이터다.
 */
public record ModelCatalogModel(
        String id,
        String displayName,
        int contextWindow,
        int maxOutputTokens,
        boolean supportsReasoning,
        boolean supportsTools,
        boolean supportsVision) {}
