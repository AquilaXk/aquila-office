package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;

public interface SaveWorkspacePort {

    SavedWorkspace save(Workspace workspace, WorkspaceMember workspaceMember);
}
