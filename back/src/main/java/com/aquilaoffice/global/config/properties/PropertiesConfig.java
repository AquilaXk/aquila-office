package com.aquilaoffice.global.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * typed configuration properties를 Spring context에 등록한다.
 */
@Configuration
@EnableConfigurationProperties({
    AuthTokenProperties.class,
    CorsProperties.class,
    ExternalTokenEncryptionProperties.class,
    GoogleOAuthProperties.class
})
public class PropertiesConfig {}
