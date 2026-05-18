package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplate;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplateCatalog;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 워크스페이스 관리자만 Agent를 생성할 수 있도록 멤버십을 검증한다.
 */
public class CreateWorkspaceAgentService implements CreateWorkspaceAgentUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final SaveWorkspaceAgentPort saveWorkspaceAgentPort;
    private final WorkspaceAgentTemplateCatalog templateCatalog;

    public CreateWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceAgentPort saveWorkspaceAgentPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.saveWorkspaceAgentPort = saveWorkspaceAgentPort;
        this.templateCatalog = new WorkspaceAgentTemplateCatalog();
    }

    @Override
    public CreateWorkspaceAgentResult createWorkspaceAgent(CreateWorkspaceAgentCommand command) {
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        WorkspaceAgent agent = WorkspaceAgent.create(
                command.workspaceId(),
                command.name(),
                command.category(),
                command.workspacePath(),
                command.requesterGoogleSub());
        List<WorkspaceAgentSkillFile> skillFiles = mergeSkillFiles(command);

        return WorkspaceAgentResultMapper.toCreateResult(saveWorkspaceAgentPort.save(agent, skillFiles));
    }

    private List<WorkspaceAgentSkillFile> mergeSkillFiles(CreateWorkspaceAgentCommand command) {
        Map<String, WorkspaceAgentSkillFile> mergedSkillFiles = new LinkedHashMap<>();
        for (WorkspaceAgentTemplate template : templateCatalog.defaultsFor(command.category())) {
            mergedSkillFiles.put(
                    template.fileName(),
                    WorkspaceAgentSkillFile.create(template.fileName(), template.content()));
        }
        for (var requestedSkillFile : command.skillFiles()) {
            WorkspaceAgentSkillFile skillFile = WorkspaceAgentSkillFile.create(
                    requestedSkillFile.fileName(),
                    requestedSkillFile.content());
            mergedSkillFiles.put(skillFile.getFileName(), skillFile);
        }
        return List.copyOf(mergedSkillFiles.values());
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireAdminMembership(Long workspaceId, String googleSub) {
        var workspace = loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }
}
