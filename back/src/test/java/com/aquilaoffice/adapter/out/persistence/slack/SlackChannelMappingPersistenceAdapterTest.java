package com.aquilaoffice.adapter.out.persistence.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.Workspace;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Slack channel mapping persistence adapter가 active mapping만 조회하는지 검증한다.
 */
@DataJpaTest
@Import(SlackChannelMappingPersistenceAdapter.class)
class SlackChannelMappingPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private SlackChannelMappingJpaRepository slackChannelMappingJpaRepository;

    @Autowired
    private SaveSlackChannelMappingPort saveSlackChannelMappingPort;

    @Autowired
    private LoadSlackChannelMappingPort loadSlackChannelMappingPort;

    @Test
    void savePersistsMappingAndLoadsActiveMappingByTeamAndChannel() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        SlackChannelMapping savedMapping = saveSlackChannelMappingPort.save(SlackChannelMapping.create(
                workspaceEntity.getId(),
                " T123 ",
                " C456 ",
                " dev-alerts ",
                " xoxb-token-secret ",
                " creator-google "));

        assertThat(savedMapping.getId()).isNotNull();
        assertThat(savedMapping.getWorkspaceId()).isEqualTo(workspaceEntity.getId());
        assertThat(savedMapping.getBotToken()).isEqualTo("xoxb-token-secret");
        assertThat(loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId("T123", "C456"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(savedMapping.getId());
                    assertThat(found.getChannelName()).isEqualTo("dev-alerts");
                    assertThat(found.getBotToken()).isEqualTo("xoxb-token-secret");
                    assertThat(found.isActive()).isTrue();
                });
    }

    @Test
    void findActiveByTeamAndChannelIgnoresInactiveMappings() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        SlackChannelMapping inactiveMapping = SlackChannelMapping.create(
                workspaceEntity.getId(),
                "T123",
                "C456",
                null,
                "xoxb-token-secret",
                "creator-google");
        inactiveMapping.deactivate();
        saveSlackChannelMappingPort.save(inactiveMapping);

        assertThat(loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId("T123", "C456")).isEmpty();
    }

    @Test
    void findActiveByWorkspaceIdReturnsOnlyWorkspaceActiveMappings() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        WorkspaceJpaEntity otherWorkspaceEntity = saveWorkspaceEntity("other-google");
        saveSlackChannelMappingPort.save(SlackChannelMapping.create(
                workspaceEntity.getId(),
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-token-secret",
                "creator-google"));
        SlackChannelMapping inactiveMapping = SlackChannelMapping.create(
                workspaceEntity.getId(),
                "T123",
                "C789",
                "old-alerts",
                "xoxb-token-secret",
                "creator-google");
        inactiveMapping.deactivate();
        saveSlackChannelMappingPort.save(inactiveMapping);
        saveSlackChannelMappingPort.save(SlackChannelMapping.create(
                otherWorkspaceEntity.getId(),
                "T999",
                "C999",
                "other-alerts",
                "xoxb-token-secret",
                "other-google"));

        List<SlackChannelMapping> mappings =
                loadSlackChannelMappingPort.findActiveByWorkspaceId(workspaceEntity.getId());

        assertThat(mappings)
                .extracting(SlackChannelMapping::getChannelId)
                .containsExactly("C456");
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
