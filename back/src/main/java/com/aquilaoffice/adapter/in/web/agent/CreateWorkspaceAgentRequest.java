package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;

/**
 * 워크스페이스 Agent 생성 HTTP 요청이다.
 */
public record CreateWorkspaceAgentRequest(
        @NotBlank String name,
        String emoji,
        String category,
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
                emoji,
                normalizeCategory(category),
                workspacePath,
                skillFiles.stream()
                        .map(WorkspaceAgentSkillFileRequest::toCommand)
                        .toList());
    }

    private static WorkspaceAgentCategory normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return WorkspaceAgentCategory.CUSTOM;
        }
        try {
            return WorkspaceAgentCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            // 선택 입력의 알 수 없는 값은 생성 호환성을 위해 CUSTOM으로 낮춘다.
            return WorkspaceAgentCategory.CUSTOM;
        }
    }
}
