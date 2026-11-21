package com.aquilaoffice.application.port.in;

/**
 * Slack OAuth callback을 완료하는 use case다.
 */
public interface CompleteSlackOAuthCallbackUseCase {

    CompleteSlackOAuthCallbackResult completeSlackOAuthCallback(CompleteSlackOAuthCallbackCommand command);
}
