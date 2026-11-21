package com.aquilaoffice.application.port.out;

/**
 * 기존 Gateway Agent의 skill 파일을 Gateway RPC로 동기화하는 outbound port다.
 */
public interface SyncWorkspaceAgentSkillFilesGatewayPort {

    SyncWorkspaceAgentSkillFilesResult syncSkillFiles(SyncWorkspaceAgentSkillFilesCommand command);
}
