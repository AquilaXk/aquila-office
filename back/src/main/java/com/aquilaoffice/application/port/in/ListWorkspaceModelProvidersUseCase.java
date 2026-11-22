package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * 워크스페이스에 등록된 AI 모델 공급자 목록을 조회하는 use case다.
 */
public interface ListWorkspaceModelProvidersUseCase {

    List<WorkspaceModelProviderResult> list(Long workspaceId, String requesterGoogleSub);
}
