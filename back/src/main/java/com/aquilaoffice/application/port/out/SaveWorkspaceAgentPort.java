package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import java.util.List;

public interface SaveWorkspaceAgentPort {

    SavedWorkspaceAgent save(WorkspaceAgent agent, List<WorkspaceAgentSkillFile> skillFiles);
}
