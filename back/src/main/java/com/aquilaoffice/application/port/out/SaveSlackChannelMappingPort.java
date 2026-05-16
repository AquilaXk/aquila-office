package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.slack.SlackChannelMapping;

/**
 * Slack channel mapping 저장 outbound port다.
 */
public interface SaveSlackChannelMappingPort {

    SlackChannelMapping save(SlackChannelMapping mapping);
}
