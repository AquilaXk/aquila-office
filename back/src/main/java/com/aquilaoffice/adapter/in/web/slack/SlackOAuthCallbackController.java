package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackCommand;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackResult;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack OAuth callback query를 application use case로 전달하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/slack/oauth/callback")
public class SlackOAuthCallbackController {

    private final CompleteSlackOAuthCallbackUseCase completeSlackOAuthCallbackUseCase;
    private final String redirectUri;

    public SlackOAuthCallbackController(
            CompleteSlackOAuthCallbackUseCase completeSlackOAuthCallbackUseCase,
            @Value("${aquila.slack.redirect-uri:http://localhost:8080/api/v1/slack/oauth/callback}")
                    String redirectUri) {
        this.completeSlackOAuthCallbackUseCase = completeSlackOAuthCallbackUseCase;
        this.redirectUri = requireNotBlank(redirectUri);
    }

    @GetMapping
    public ResponseEntity<RsData<SlackOAuthCallbackResponse>> completeSlackOAuthCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error) {
        rejectOAuthError(error);
        String normalizedCode = requireQueryParam(code);
        String normalizedState = requireQueryParam(state);

        try {
            CompleteSlackOAuthCallbackResult result = completeSlackOAuthCallbackUseCase.completeSlackOAuthCallback(
                    new CompleteSlackOAuthCallbackCommand(
                            normalizedCode,
                            normalizedState,
                            redirectUri));
            return ResponseEntity.ok(RsData.ok(SlackOAuthCallbackResponse.from(result)));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void rejectOAuthError(String error) {
        if (error != null && !error.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private String requireQueryParam(String value) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return value.trim();
    }

    private String requireNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("redirectUri must not be blank");
        }
        return value.trim();
    }
}
