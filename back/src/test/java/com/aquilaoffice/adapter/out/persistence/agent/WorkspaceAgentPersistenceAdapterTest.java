package com.aquilaoffice.adapter.out.persistence.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Agent persistence adapter가 Agent와 skill 파일을 저장하고 active Agent만 조회하는지 검증한다.
 */
@DataJpaTest
@Import(WorkspaceAgentPersistenceAdapter.class)
class WorkspaceAgentPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceAgentJpaRepository workspaceAgentJpaRepository;

    @Autowired
    private WorkspaceAgentSkillFileJpaRepository workspaceAgentSkillFileJpaRepository;

    @Autowired
    private SaveWorkspaceAgentPort saveWorkspaceAgentPort;

    @Autowired
    private LoadWorkspaceAgentPort loadWorkspaceAgentPort;

    @Autowired
    private DeleteWorkspaceAgentPort deleteWorkspaceAgentPort;

    @Test
    void savePersistsAgentAndSkillFilesThenLoadsActiveList() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        var saved = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(
                        workspaceEntity.getId(),
                        " Backend Agent ",
                        WorkspaceAgentCategory.BACKEND,
                        " ",
                        " creator-google "),
                List.of(WorkspaceAgentSkillFile.create(" AGENTS.md ", "skill")));

        assertThat(saved.agentId()).isNotNull();
        assertThat(saved.workspaceId()).isEqualTo(workspaceEntity.getId());
        assertThat(saved.name()).isEqualTo("Backend Agent");
        assertThat(saved.category()).isEqualTo(WorkspaceAgentCategory.BACKEND);
        assertThat(saved.workspacePath()).isEqualTo(".aquila/workspaces/" + workspaceEntity.getId());
        assertThat(saved.status()).isEqualTo(WorkspaceAgentStatus.CREATING);
        assertThat(saved.skillFiles()).singleElement().satisfies(skillFile -> {
            assertThat(skillFile.fileName()).isEqualTo("AGENTS.md");
            assertThat(skillFile.syncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.PENDING);
        });

        assertThat(loadWorkspaceAgentPort.findActiveByWorkspaceId(workspaceEntity.getId()))
                .singleElement()
                .satisfies(agent -> assertThat(agent.agentId()).isEqualTo(saved.agentId()));
    }

    @Test
    void findActiveDetailReturnsAgentWithSkillFiles() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var saved = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(
                        workspaceEntity.getId(),
                        "QA Agent",
                        WorkspaceAgentCategory.QA,
                        ".aquila/workspaces/custom",
                        "creator-google"),
                List.of(
                        WorkspaceAgentSkillFile.create("QA.md", "qa"),
                        WorkspaceAgentSkillFile.create("COMMON.md", "common")));

        assertThat(loadWorkspaceAgentPort.findActiveByWorkspaceIdAndAgentId(workspaceEntity.getId(), saved.agentId()))
                .hasValueSatisfying(agent -> {
                    assertThat(agent.name()).isEqualTo("QA Agent");
                    assertThat(agent.skillFiles()).hasSize(2);
                });
    }

    @Test
    void disableExcludesAgentFromActiveQueries() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var saved = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(
                        workspaceEntity.getId(),
                        "Frontend Agent",
                        WorkspaceAgentCategory.FRONTEND,
                        ".aquila/workspaces/frontend",
                        "creator-google"),
                List.of());

        deleteWorkspaceAgentPort.disable(workspaceEntity.getId(), saved.agentId());

        assertThat(loadWorkspaceAgentPort.findActiveByWorkspaceId(workspaceEntity.getId())).isEmpty();
        assertThat(loadWorkspaceAgentPort.findActiveByWorkspaceIdAndAgentId(workspaceEntity.getId(), saved.agentId()))
                .isEmpty();
        assertThat(workspaceAgentJpaRepository.findById(saved.agentId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(WorkspaceAgentStatus.DISABLED));
    }

    @Test
    void saveRejectsMissingWorkspaceEntity() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> saveWorkspaceAgentPort.save(
                        WorkspaceAgent.create(404L, "Agent", WorkspaceAgentCategory.CUSTOM, null, "google-1"),
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
        assertThat(workspaceAgentJpaRepository.count()).isZero();
        assertThat(workspaceAgentSkillFileJpaRepository.count()).isZero();
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
