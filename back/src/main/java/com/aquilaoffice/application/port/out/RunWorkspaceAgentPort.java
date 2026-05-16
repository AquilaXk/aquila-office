package com.aquilaoffice.application.port.out;

public interface RunWorkspaceAgentPort {

    WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command);
}
