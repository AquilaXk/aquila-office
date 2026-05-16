package com.aquilaoffice.application.service.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.SlackReplyRequest;
import org.junit.jupiter.api.Test;

class SlackReplyRequestTest {

    @Test
    void fromSourceRefParsesTeamChannelAndThreadTs() {
        SlackReplyRequest request = SlackReplyRequest.fromSourceRef(
                " T123:C456:1715841000.000100 ",
                " 작업이 완료되었습니다. ",
                " execution-701 ");

        assertThat(request.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
        assertThat(request.teamId()).isEqualTo("T123");
        assertThat(request.channelId()).isEqualTo("C456");
        assertThat(request.threadTs()).isEqualTo("1715841000.000100");
        assertThat(request.text()).isEqualTo("작업이 완료되었습니다.");
        assertThat(request.deduplicationKey()).isEqualTo("execution-701");
    }

    @Test
    void fromSourceRefRejectsMalformedSourceRef() {
        assertThatThrownBy(() -> SlackReplyRequest.fromSourceRef(
                        "T123:C456",
                        "작업이 완료되었습니다.",
                        "execution-701"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceRef must be formatted as teamId:channelId:threadTs");
    }
}
