package com.aquilaoffice.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 다중 AI 모델 공급자 연동 시 로컬 루프백 호스트 허용 여부를 설정한다.
 */
@ConfigurationProperties(prefix = "aquila.model-provider")
public record ModelProviderProperties(boolean allowLocalHosts) {}
