package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;

/**
 * Agent 생성·수정 시 함께 저장할 skill 파일 입력이다.
 */
public record WorkspaceAgentSkillFileCommand(String fileName, String content) {

    public WorkspaceAgentSkillFileCommand {
        fileName = WorkspaceAgentSkillFile.validateFileName(fileName);
        content = WorkspaceAgentSkillFile.validateContent(content);
    }
}
