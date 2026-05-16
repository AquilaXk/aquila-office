package com.aquilaoffice.domain.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * WorkspaceAgent 도메인이 Agent 생성 입력을 정규화하고 상태 전이를 보호하는지 검증한다.
 */
class WorkspaceAgentTest {

    @Test
    void createDefaultsCategoryAndWorkspacePath() {
        WorkspaceAgent agent = WorkspaceAgent.create(
                101L,
                " Backend Agent ",
                null,
                " ",
                " google-1 ");

        assertThat(agent.getWorkspaceId()).isEqualTo(101L);
        assertThat(agent.getName()).isEqualTo("Backend Agent");
        assertThat(agent.getCategory()).isEqualTo(WorkspaceAgentCategory.CUSTOM);
        assertThat(agent.getWorkspacePath()).isEqualTo(".aquila/workspaces/101");
        assertThat(agent.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(agent.getStatus()).isEqualTo(WorkspaceAgentStatus.CREATING);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> WorkspaceAgent.create(
                        101L,
                        " ",
                        WorkspaceAgentCategory.BACKEND,
                        ".aquila/workspaces/101",
                        "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void statusTransitionsNormalizeExternalIdAndErrors() {
        WorkspaceAgent agent = WorkspaceAgent.create(
                101L,
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/101",
                "google-1");

        agent.markReady(" gateway-agent-1 ");
        assertThat(agent.getStatus()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(agent.getOpenClawAgentId()).isEqualTo("gateway-agent-1");
        assertThat(agent.getSyncError()).isNull();

        agent.markSyncFailed(" sync failed ");
        assertThat(agent.getStatus()).isEqualTo(WorkspaceAgentStatus.SYNC_FAILED);
        assertThat(agent.getSyncError()).isEqualTo("sync failed");

        agent.disable();
        assertThat(agent.getStatus()).isEqualTo(WorkspaceAgentStatus.DISABLED);
        assertThat(agent.getSyncError()).isNull();
    }

    @Test
    void skillFileDefaultsToPendingAndTrimsName() {
        WorkspaceAgentSkillFile skillFile = WorkspaceAgentSkillFile.create(" AGENTS.md ", "content");

        assertThat(skillFile.getFileName()).isEqualTo("AGENTS.md");
        assertThat(skillFile.getContent()).isEqualTo("content");
        assertThat(skillFile.getSyncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.PENDING);
        assertThat(skillFile.getSyncError()).isNull();
    }
}
