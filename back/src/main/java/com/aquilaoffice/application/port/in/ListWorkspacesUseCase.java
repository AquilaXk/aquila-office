package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * 워크스페이스 목록 조회 기능을 외부 입력 어댑터에 공개하는 inbound port다.
 */
public interface ListWorkspacesUseCase {

    List<ListWorkspacesResult> listWorkspaces(ListWorkspacesQuery query);
}
