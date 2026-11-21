package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlackAgentCommandParserTest {

    private final SlackAgentCommandParser parser = new SlackAgentCommandParser();

    @Test
    void parseRemovesMentionAndExtractsAgentIdAndMessage() {
        SlackAgentCommandParser.ParsedCommand parsed =
                parser.parse("<@U123ABC> /agent 501  결제 오류를 수정해주세요");

        assertThat(parsed.matched()).isTrue();
        assertThat(parsed.agentId()).isEqualTo(501L);
        assertThat(parsed.message()).isEqualTo("결제 오류를 수정해주세요");
    }

    @Test
    void parseReturnsUnmatchedWhenCommandIsMissing() {
        SlackAgentCommandParser.ParsedCommand parsed = parser.parse("<@U123ABC> 배포 상태 알려줘");

        assertThat(parsed.matched()).isFalse();
        assertThat(parsed.agentId()).isNull();
        assertThat(parsed.message()).isEqualTo("배포 상태 알려줘");
    }

    @Test
    void parseReturnsUnmatchedForBlankText() {
        SlackAgentCommandParser.ParsedCommand parsed = parser.parse("   ");

        assertThat(parsed.matched()).isFalse();
        assertThat(parsed.agentId()).isNull();
        assertThat(parsed.message()).isEmpty();
    }
}
