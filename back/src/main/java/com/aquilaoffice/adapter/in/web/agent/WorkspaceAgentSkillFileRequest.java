package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Agent 생성 요청에 포함되는 skill 파일 입력이다.
 */
public record WorkspaceAgentSkillFileRequest(
        @NotBlank String fileName,
        String content) {

    public WorkspaceAgentSkillFileCommand toCommand() {
        return new WorkspaceAgentSkillFileCommand(fileName, content);
    }
}
