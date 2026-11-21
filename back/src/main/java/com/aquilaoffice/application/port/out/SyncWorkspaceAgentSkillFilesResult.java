package com.aquilaoffice.application.port.out;

import java.util.List;

/**
 * Gateway Agent skill 파일 동기화 결과다.
 */
public record SyncWorkspaceAgentSkillFilesResult(
        boolean success,
        String failureReason,
        List<SkillFileResult> skillFiles) {

    public SyncWorkspaceAgentSkillFilesResult {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public record SkillFileResult(String fileName, boolean success, String failureReason) {}
}
