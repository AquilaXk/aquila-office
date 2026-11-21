package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent skill 파일 동기화 상태와 본문 응답이다.
 */
@Schema(description = "Agent skill 파일 응답")
public record WorkspaceAgentSkillFileResponse(
        @Schema(description = "skill 파일 id", example = "601")
        Long skillFileId,
        @Schema(description = "skill 파일 이름", example = "AGENTS.md")
        String fileName,
        @Schema(description = "skill 파일 본문")
        String content,
        @Schema(description = "Gateway 동기화 상태", example = "SYNCED")
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        @Schema(description = "동기화 실패 사유", nullable = true)
        String syncError) {

    public WorkspaceAgentSkillFileResponse(
            Long skillFileId,
            String fileName,
            WorkspaceAgentSkillFileSyncStatus syncStatus,
            String syncError) {
        this(skillFileId, fileName, null, syncStatus, syncError);
    }

    public static WorkspaceAgentSkillFileResponse from(WorkspaceAgentSkillFileResult result) {
        return new WorkspaceAgentSkillFileResponse(
                result.skillFileId(),
                result.fileName(),
                result.content(),
                result.syncStatus(),
                result.syncError());
    }
}
