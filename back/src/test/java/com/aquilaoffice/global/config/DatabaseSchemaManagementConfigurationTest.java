package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DatabaseSchemaManagementConfigurationTest {

    @Test
    void backendUsesFlywayForPostgresSchemaChanges() throws IOException {
        Path root = repositoryRoot();
        String buildScript = Files.readString(root.resolve("back/build.gradle.kts"));

        assertThat(buildScript)
                .contains("org.flywaydb:flyway-core")
                .contains("org.flywaydb:flyway-database-postgresql");
    }

    @Test
    void runtimeSchemaIsValidatedAfterSqlChangesRun() throws IOException {
        String applicationConfig = Files.readString(repositoryRoot().resolve("back/src/main/resources/application.yml"));

        assertThat(applicationConfig)
                .contains("ddl-auto: validate")
                .contains("flyway:")
                .contains("enabled: ${SPRING_FLYWAY_ENABLED:true}")
                .contains("baseline-on-migrate: ${SPRING_FLYWAY_BASELINE_ON_MIGRATE:true}");
    }

    @Test
    void localH2ProfilesKeepJpaOwnedTestSchema() throws IOException {
        Path root = repositoryRoot();

        assertThat(Files.readString(root.resolve("back/src/test/resources/application.yml")))
                .contains("ddl-auto: create-drop")
                .contains("flyway:")
                .contains("enabled: false");
        assertThat(Files.readString(root.resolve("back/src/main/resources/application-smoke.yml")))
                .contains("ddl-auto: create-drop")
                .contains("flyway:")
                .contains("enabled: false");
    }

    @Test
    void sqlHistoryStartsFromCurrentJpaSchemaAndDropsLegacyRefreshTokenTable() throws IOException {
        Path root = repositoryRoot();
        String initialSchema = Files.readString(root.resolve("back/src/main/resources/db/migration/V1__init_schema.sql"));
        String legacyCleanup = Files.readString(root.resolve(
                "back/src/main/resources/db/migration/V2__drop_legacy_refresh_tokens.sql"));

        assertThat(initialSchema)
                .contains("create table refresh_token_sessions")
                .contains("create table slack_channel_mappings");
        assertThat(legacyCleanup)
                .contains("alter table if exists refresh_tokens")
                .contains("drop table if exists refresh_tokens");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
