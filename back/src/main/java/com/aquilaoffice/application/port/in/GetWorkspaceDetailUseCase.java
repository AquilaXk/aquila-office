package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 상세 조회 기능을 외부 입력 어댑터에 공개하는 inbound port다.
 */
public interface GetWorkspaceDetailUseCase {

    GetWorkspaceDetailResult getWorkspaceDetail(GetWorkspaceDetailQuery query);
}
