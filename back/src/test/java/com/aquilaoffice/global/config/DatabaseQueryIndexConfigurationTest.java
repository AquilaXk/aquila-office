package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseQueryIndexConfigurationTest {

    @Test
    void fkBackedQueriesHaveExplicitIndexes() throws IOException {
        String indexSql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V3__add_query_indexes.sql"));

        List<String> requiredIndexes = List.of(
                "idx_workspaces_created_by_member_id",
                "idx_workspace_members_member_id",
                "idx_workspace_invites_workspace_id",
                "idx_workspace_gateway_bindings_workspace_id",
                "idx_github_credentials_workspace_id",
                "idx_slack_channel_mappings_workspace_id",
                "idx_workspace_agents_workspace_id",
                "idx_workspace_agent_skill_files_agent_id",
                "idx_task_executions_task_id",
                "idx_chat_sessions_workspace_id",
                "idx_chat_messages_workspace_id",
                "idx_orchestration_sessions_workspace_id",
                "idx_orchestration_steps_session_id");

        assertThat(indexSql).contains(requiredIndexes.toArray(String[]::new));
    }

    @Test
    void highVolumeTimelineQueriesHaveCompositeIndexes() throws IOException {
        String indexSql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V3__add_query_indexes.sql"));

        assertThat(indexSql)
                .contains("idx_chat_messages_session_created_at")
                .contains("idx_task_execution_logs_execution_created_at")
                .contains("idx_tasks_workspace_status")
                .contains("idx_task_executions_workspace_status");
    }

    @Test
    void v4CompositeIndexesAndGovernanceColumnsAreDefined() throws IOException {
        String v4Sql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V4__add_composite_indexes_and_ops_governance.sql"));

        assertThat(v4Sql)
                .contains("idx_workspaces_id_deleted_at")
                .contains("idx_workspace_members_workspace_role_joined")
                .contains("idx_workspace_invites_workspace_status_id")
                .contains("idx_workspace_agents_workspace_status_id")
                .contains("idx_workspace_agent_skill_files_agent_id_id")
                .contains("idx_tasks_workspace_assigned_status_id")
                .contains("idx_task_executions_workspace_task_created_at")
                .contains("daily_token_limit")
                .contains("duration_ms")
                .contains("prompt_tokens")
                .contains("completion_tokens")
                .contains("model_name");
    }

    @Test
    void v5ModelProvidersAndCompositeQueryIndexesAreDefined() throws IOException {
        String v5Sql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V5__add_ai_model_providers_and_query_indexes.sql"));

        assertThat(v5Sql)
                .contains("create table workspace_model_providers")
                .contains("idx_workspace_model_providers_ws_priority")
                .contains("fk_workspace_agents_model_provider")
                .contains("idx_task_executions_workspace_created_id")
                .contains("idx_tasks_workspace_id_desc")
                .contains("idx_workspace_invites_workspace_id_desc")
                .contains("idx_chat_messages_workspace_session_id")
                .contains("idx_chat_messages_workspace_task_id")
                .contains("idx_orchestration_sessions_workspace_id_desc");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
