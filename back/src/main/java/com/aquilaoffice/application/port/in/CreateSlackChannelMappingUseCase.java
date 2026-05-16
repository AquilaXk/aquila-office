package com.aquilaoffice.application.port.in;

/**
 * Slack channel mapping 생성 use case다.
 */
public interface CreateSlackChannelMappingUseCase {

    CreateSlackChannelMappingResult createSlackChannelMapping(CreateSlackChannelMappingCommand command);
}
