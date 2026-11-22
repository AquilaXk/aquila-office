package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;

/**
 * 워크스페이스 Agent 생성 HTTP 요청이다.
 */
@Schema(description = "워크스페이스 Agent 생성 요청")
public record CreateWorkspaceAgentRequest(
        @NotBlank String name,
        String emoji,
        String category,
        String workspacePath,
        @Valid @Size(max = 20) List<WorkspaceAgentSkillFileRequest> skillFiles,
        @Schema(nullable = true) Long modelProviderId,
        @Schema(nullable = true) @Size(max = 160) String modelId,
        @Schema(nullable = true) @Size(max = 160) String fallbackModelId) {

    public CreateWorkspaceAgentRequest {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public CreateWorkspaceAgentRequest(
            String name,
            String emoji,
            String category,
            String workspacePath,
            List<WorkspaceAgentSkillFileRequest> skillFiles) {
        this(name, emoji, category, workspacePath, skillFiles, null, null, null);
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
                        .toList(),
                modelProviderId,
                modelId,
                fallbackModelId);
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
