package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentCommand;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 워크스페이스 Agent 수정 HTTP 요청이다.
 */
@Schema(description = "워크스페이스 Agent 수정 요청")
public record UpdateWorkspaceAgentRequest(
        @Schema(description = "Agent 이름", example = "Backend Agent")
        @NotBlank
        @Size(max = 120)
        String name,
        @Schema(description = "Agent 담당 영역", example = "BACKEND", nullable = true)
        String category,
        @Schema(description = "Agent 작업 경로", example = ".aquila/workspaces/101", nullable = true)
        @Size(max = 512)
        @Pattern(regexp = "^(?!.*\\.\\.)[^\\u0000]*$")
        String workspacePath,
        @Schema(description = "교체할 skill 파일 목록 (최대 20개, 생략 시 기존 유지)", nullable = true)
        @Size(max = 20)
        List<@NotNull @Valid WorkspaceAgentSkillFileRequest> skillFiles,
        @Schema(description = "선택적 emoji(무시됨)", nullable = true)
        String emoji) {

    public UpdateWorkspaceAgentRequest {
        if (skillFiles != null) {
            if (skillFiles.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("skillFiles must not contain null");
            }
            skillFiles = List.copyOf(skillFiles);
        }
    }

    public UpdateWorkspaceAgentRequest(
            String name,
            String category,
            String workspacePath,
            List<WorkspaceAgentSkillFileRequest> skillFiles) {
        this(name, category, workspacePath, skillFiles, null);
    }

    public UpdateWorkspaceAgentCommand toCommand(Long workspaceId, Long agentId, String requesterGoogleSub) {
        return new UpdateWorkspaceAgentCommand(
                workspaceId,
                agentId,
                requesterGoogleSub,
                name,
                parseOptionalCategory(category),
                workspacePath,
                skillFiles == null
                        ? null
                        : skillFiles.stream()
                                .map(WorkspaceAgentSkillFileRequest::toCommand)
                                .toList());
    }

    private static WorkspaceAgentCategory parseOptionalCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return WorkspaceAgentCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
    }
}
