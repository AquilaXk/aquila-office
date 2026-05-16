package com.aquilaoffice.application.port.out;

/**
 * Slack OAuth authorization code를 bot token 정보로 교환하는 outbound port다.
 */
public interface ExchangeSlackOAuthCodePort {

    SlackOAuthCodeExchangeResult exchangeCode(SlackOAuthCodeExchangeRequest request);
}
