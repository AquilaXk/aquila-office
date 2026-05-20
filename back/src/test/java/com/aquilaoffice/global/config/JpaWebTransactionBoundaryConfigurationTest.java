package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JpaWebTransactionBoundaryConfigurationTest {

    @Test
    void runtimeDisablesOpenEntityManagerInView() throws IOException {
        String applicationConfig = Files.readString(repositoryRoot().resolve("back/src/main/resources/application.yml"));

        assertThat(applicationConfig)
                .contains("jpa:")
                .contains("open-in-view: false");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
