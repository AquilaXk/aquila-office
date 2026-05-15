package com.aquilaoffice.application.port.out;

/**
 * persistence adapter가 워크스페이스 정보 수정 후 application 계층에 돌려주는 결과값이다.
 */
public record UpdatedWorkspace(Long workspaceId, String name, String description, String creatorGoogleSub) {}
