package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Agent 생성·수정 요청에 포함되는 skill 파일 입력이다.
 */
@Schema(description = "Agent skill 파일 요청")
public record WorkspaceAgentSkillFileRequest(
        @Schema(description = "skill 파일 이름", example = "AGENTS.md")
        @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "^(?!\\s*\\.\\s*$)(?!.*\\.\\.)[^/\\\\\\u0000]+$")
        String fileName,
        @Schema(description = "skill 파일 본문 (최대 65536자)")
        @Size(max = 65536)
        String content) {

    public WorkspaceAgentSkillFileCommand toCommand() {
        return new WorkspaceAgentSkillFileCommand(fileName, content);
    }
}
