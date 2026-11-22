package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

class DatabaseSchemaManagementConfigurationTest {

    @Test
    void backendUsesSpringBootFlywayStarterForPostgresSchemaChanges() throws IOException {
        Path root = repositoryRoot();
        String buildScript = Files.readString(root.resolve("back/build.gradle.kts"));

        assertThat(buildScript)
                .contains("org.springframework.boot:spring-boot-starter-flyway")
                .contains("org.flywaydb:flyway-database-postgresql")
                .doesNotContain("implementation(\"org.flywaydb:flyway-core\")");
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

    @Test
    void flywayV1ThroughV5ExecuteCleanlyAgainstH2PostgreSqlMode() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:flyway_v1_v5_verify;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        Path migrationDir = repositoryRoot().resolve("back/src/main/resources/db/migration");

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("filesystem:" + migrationDir.toAbsolutePath())
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.migrationsExecuted).isEqualTo(5);

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(5);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "select column_name from information_schema.columns "
                            + "where table_name = 'workspace_model_providers'")) {
                java.util.List<String> columns = new java.util.ArrayList<>();
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
                assertThat(columns).contains(
                        "id",
                        "workspace_id",
                        "provider_kind",
                        "wire_protocol",
                        "display_name",
                        "base_url",
                        "api_key",
                        "default_model",
                        "enabled_models_json",
                        "priority_order",
                        "enabled",
                        "last_status",
                        "last_latency_ms",
                        "last_checked_at",
                        "last_error",
                        "deleted_at");
            }
            try (ResultSet rs = statement.executeQuery(
                    "select column_name from information_schema.columns "
                            + "where table_name = 'workspace_agents'")) {
                java.util.List<String> agentColumns = new java.util.ArrayList<>();
                while (rs.next()) {
                    agentColumns.add(rs.getString(1));
                }
                assertThat(agentColumns).contains("model_provider_id", "model_id", "fallback_model_id");
            }
        }
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
