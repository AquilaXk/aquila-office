package com.aquilaoffice.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 어댑터 전반에서 Jackson ObjectMapper 인스턴스를 매번 새로 생성하지 않고 공유한다.
 */
@Configuration
public class SharedJacksonConfig {

    @Bean
    public ObjectMapper sharedJacksonObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
