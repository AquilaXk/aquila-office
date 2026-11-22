package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.model.WorkspaceModelProvider;

/**
 * 워크스페이스 AI 모델 공급자 설정을 저장·갱신하는 outbound port다.
 */
public interface SaveWorkspaceModelProviderPort {

    WorkspaceModelProvider save(WorkspaceModelProvider provider);
}
