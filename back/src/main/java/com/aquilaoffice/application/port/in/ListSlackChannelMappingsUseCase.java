package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * Slack channel mapping 목록 조회 use case다.
 */
public interface ListSlackChannelMappingsUseCase {

    List<ListSlackChannelMappingsResult> listSlackChannelMappings(ListSlackChannelMappingsQuery query);
}
