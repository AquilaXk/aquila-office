package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import java.util.List;

/**
 * 수정된 Agent 메타데이터와 교체할 skill 파일을 저장소에 반영하는 outbound port다.
 */
public interface UpdateWorkspaceAgentPort {

    SavedWorkspaceAgent update(WorkspaceAgent agent, List<WorkspaceAgentSkillFile> replacedSkillFiles);
}
