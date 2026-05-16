package com.aquilaoffice.application.port.out;

/**
 * Slack thread 응답 전송 outbound port다.
 */
public interface SendSlackReplyPort {

    void sendReply(SlackReplyRequest request);
}
