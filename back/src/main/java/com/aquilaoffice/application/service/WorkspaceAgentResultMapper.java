package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;

final class WorkspaceAgentResultMapper {

    private WorkspaceAgentResultMapper() {}

    static CreateWorkspaceAgentResult toCreateResult(SavedWorkspaceAgent agent) {
        return new CreateWorkspaceAgentResult(
                agent.agentId(),
                agent.workspaceId(),
                agent.name(),
                agent.category(),
                agent.openClawAgentId(),
                agent.workspacePath(),
                agent.status(),
                agent.syncError(),
                agent.createdByGoogleSub(),
                agent.createdAt(),
                agent.updatedAt(),
                agent.skillFiles().stream()
                        .map(WorkspaceAgentResultMapper::toSkillFileResult)
                        .toList(),
                agent.modelProviderId(),
                agent.modelId(),
                agent.fallbackModelId());
    }

    static ListWorkspaceAgentsResult toListResult(SavedWorkspaceAgent agent) {
        return new ListWorkspaceAgentsResult(
                agent.agentId(),
                agent.workspaceId(),
                agent.name(),
                agent.category(),
                agent.openClawAgentId(),
                agent.workspacePath(),
                agent.status(),
                agent.syncError(),
                agent.createdByGoogleSub(),
                agent.createdAt(),
                agent.updatedAt(),
                agent.skillFiles().stream()
                        .map(WorkspaceAgentResultMapper::toSkillFileResult)
                        .toList(),
                agent.modelProviderId(),
                agent.modelId(),
                agent.fallbackModelId());
    }

    static GetWorkspaceAgentResult toGetResult(SavedWorkspaceAgent agent) {
        return new GetWorkspaceAgentResult(
                agent.agentId(),
                agent.workspaceId(),
                agent.name(),
                agent.category(),
                agent.openClawAgentId(),
                agent.workspacePath(),
                agent.status(),
                agent.syncError(),
                agent.createdByGoogleSub(),
                agent.createdAt(),
                agent.updatedAt(),
                agent.skillFiles().stream()
                        .map(WorkspaceAgentResultMapper::toSkillFileResult)
                        .toList(),
                agent.modelProviderId(),
                agent.modelId(),
                agent.fallbackModelId());
    }

    static UpdateWorkspaceAgentResult toUpdateResult(SavedWorkspaceAgent agent) {
        return new UpdateWorkspaceAgentResult(
                agent.agentId(),
                agent.workspaceId(),
                agent.name(),
                agent.category(),
                agent.openClawAgentId(),
                agent.workspacePath(),
                agent.status(),
                agent.syncError(),
                agent.createdByGoogleSub(),
                agent.createdAt(),
                agent.updatedAt(),
                agent.skillFiles().stream()
                        .map(WorkspaceAgentResultMapper::toSkillFileResult)
                        .toList(),
                agent.modelProviderId(),
                agent.modelId(),
                agent.fallbackModelId());
    }

    private static WorkspaceAgentSkillFileResult toSkillFileResult(SavedWorkspaceAgentSkillFile skillFile) {
        return new WorkspaceAgentSkillFileResult(
                skillFile.skillFileId(),
                skillFile.fileName(),
                skillFile.content(),
                skillFile.syncStatus(),
                skillFile.syncError());
    }
}
