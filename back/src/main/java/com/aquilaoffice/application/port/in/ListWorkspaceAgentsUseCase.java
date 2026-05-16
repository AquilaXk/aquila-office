package com.aquilaoffice.application.port.in;

import java.util.List;

public interface ListWorkspaceAgentsUseCase {

    List<ListWorkspaceAgentsResult> listWorkspaceAgents(ListWorkspaceAgentsQuery query);
}
