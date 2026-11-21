package com.aquilaoffice.application.port.out;

import java.util.List;

/**
 * 기존 Gateway Agent의 skill 파일을 일괄 동기화할 때 사용하는 command다.
 */
public record SyncWorkspaceAgentSkillFilesCommand(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String openClawAgentId,
        List<SkillFile> skillFiles) {

    public SyncWorkspaceAgentSkillFilesCommand {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public record SkillFile(String fileName, String content) {}
}
