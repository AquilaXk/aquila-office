package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 워크스페이스 Agent 생성 HTTP 요청이다.
 */
public record CreateWorkspaceAgentRequest(
        @NotBlank String name,
        WorkspaceAgentCategory category,
        String workspacePath,
        @Valid List<WorkspaceAgentSkillFileRequest> skillFiles) {

    public CreateWorkspaceAgentRequest {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public CreateWorkspaceAgentCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateWorkspaceAgentCommand(
                workspaceId,
                requesterGoogleSub,
                name,
                category,
                workspacePath,
                skillFiles.stream()
                        .map(WorkspaceAgentSkillFileRequest::toCommand)
                        .toList());
    }
}
