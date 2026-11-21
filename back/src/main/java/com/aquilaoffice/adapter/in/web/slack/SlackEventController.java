package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.service.WorkflowMetrics;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack 이벤트를 workspace chat/task 실행 요청으로 연결하는 inbound web adapter다.
 */
@RestController
public class SlackEventController {

    private static final String SLACK_ROUTE_GLOBAL = "global";
    private static final String SLACK_ROUTE_WORKSPACE = "workspace";

    private final ReceiveSlackEventUseCase receiveSlackEventUseCase;
    private final SlackSignatureVerifier slackSignatureVerifier;
    private final WorkflowMetrics workflowMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackEventController(
            ReceiveSlackEventUseCase receiveSlackEventUseCase,
            SlackSignatureVerifier slackSignatureVerifier,
            ObjectProvider<WorkflowMetrics> workflowMetricsProvider) {
        this.receiveSlackEventUseCase =
                Objects.requireNonNull(receiveSlackEventUseCase, "receiveSlackEventUseCase must not be null");
        this.slackSignatureVerifier =
                Objects.requireNonNull(slackSignatureVerifier, "slackSignatureVerifier must not be null");
        this.workflowMetrics = workflowMetricsProvider.getIfAvailable(WorkflowMetrics::noop);
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/slack/events")
    public ResponseEntity<Object> receiveSlackEvent(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @RequestHeader(name = "X-Slack-Request-Timestamp", required = false) String slackRequestTimestamp,
            @RequestHeader(name = "X-Slack-Signature", required = false) String slackSignature,
            @RequestBody String rawBody) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        SlackEventRequest request =
                parseVerifiedRequest(SLACK_ROUTE_WORKSPACE, slackRequestTimestamp, slackSignature, rawBody);
        return handleSlackEvent(request.toCommand(workspaceId, normalizedRequesterGoogleSub));
    }

    @PostMapping("/api/v1/slack/events")
    public ResponseEntity<Object> receiveMappedSlackEvent(
            @RequestHeader(name = "X-Slack-Request-Timestamp", required = false) String slackRequestTimestamp,
            @RequestHeader(name = "X-Slack-Signature", required = false) String slackSignature,
            @RequestBody String rawBody) {
        SlackEventRequest request =
                parseVerifiedRequest(SLACK_ROUTE_GLOBAL, slackRequestTimestamp, slackSignature, rawBody);
        return handleSlackEvent(request.toCommand(null, null));
    }

    private ResponseEntity<Object> handleSlackEvent(ReceiveSlackEventCommand command) {
        try {
            ReceiveSlackEventResult result = receiveSlackEventUseCase.receiveSlackEvent(command);
            if (result.challenge() != null) {
                // Slack URL verification은 RsData envelope 없이 challenge JSON을 그대로 반환해야 한다.
                return ResponseEntity.ok(Map.of("challenge", result.challenge()));
            }
            return ResponseEntity.ok(RsData.ok(SlackEventResponse.from(result)));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            workflowMetrics.recordSlackProcessingFailure("use_case");
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private SlackEventRequest parseVerifiedRequest(String route, String timestamp, String signature, String rawBody) {
        workflowMetrics.recordSlackEventReceived(route);
        try {
            slackSignatureVerifier.verifyOrThrow(timestamp, signature, rawBody);
        } catch (IllegalArgumentException exception) {
            workflowMetrics.recordSlackSignatureFailure(route);
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            workflowMetrics.recordSlackProcessingFailure("signature_config");
            throw new ServiceException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            return objectMapper.readValue(rawBody, SlackEventRequest.class);
        } catch (JsonProcessingException exception) {
            workflowMetrics.recordSlackProcessingFailure("payload_parse");
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
