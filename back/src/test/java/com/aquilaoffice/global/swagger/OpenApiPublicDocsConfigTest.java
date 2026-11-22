package com.aquilaoffice.global.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * aquila.ops.public-docs-and-metrics=true 설정 시 OpenAPI 문서와 Swagger UI가 공개되는지 검증한다.
 */
@SpringBootTest(properties = "aquila.ops.public-docs-and-metrics=true")
@AutoConfigureMockMvc
class OpenApiPublicDocsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAndSwaggerUiArePublicWhenExplicitlyEnabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Aquila Office API"));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
