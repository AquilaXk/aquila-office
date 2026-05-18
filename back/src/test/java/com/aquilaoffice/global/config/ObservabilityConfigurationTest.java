package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ObservabilityConfigurationTest {

    @Test
    void applicationExposesHealthMetricsAndPrometheusActuatorEndpoints() throws IOException {
        String applicationConfig = Files.readString(repositoryRoot().resolve("back/src/main/resources/application.yml"));

        assertThat(applicationConfig)
                .contains("management:")
                .contains("include: health,info,metrics,prometheus")
                .contains("show-details: ${MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS:when_authorized}");
    }

    @Test
    void backendIncludesPrometheusRegistryRuntime() throws IOException {
        String buildScript = Files.readString(repositoryRoot().resolve("back/build.gradle.kts"));

        assertThat(buildScript)
                .contains("spring-boot-starter-actuator")
                .contains("io.micrometer:micrometer-registry-prometheus");
    }

    @Test
    void prometheusAndMetricsActuatorEndpointsArePublicForScraping() throws IOException {
        Path root = repositoryRoot();
        String securityConfig = Files.readString(root.resolve(
                "back/src/main/java/com/aquilaoffice/global/security/SecurityConfig.java"));
        String authFilter = Files.readString(root.resolve(
                "back/src/main/java/com/aquilaoffice/global/security/AccessTokenAuthenticationFilter.java"));

        assertThat(securityConfig)
                .contains("\"/actuator/health\", \"/actuator/health/**\"")
                .contains("\"/actuator/metrics\", \"/actuator/metrics/**\"")
                .contains("\"/actuator/prometheus\"");
        assertThat(authFilter)
                .contains("path.startsWith(\"/actuator/health\")")
                .contains("path.startsWith(\"/actuator/metrics\")")
                .contains("path.startsWith(\"/actuator/prometheus\")");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
