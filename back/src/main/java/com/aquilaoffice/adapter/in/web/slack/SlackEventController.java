package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack 이벤트를 workspace chat/task 실행 요청으로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/slack")
public class SlackEventController {

    private final ReceiveSlackEventUseCase receiveSlackEventUseCase;
    private final SlackSignatureVerifier slackSignatureVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackEventController(
            ReceiveSlackEventUseCase receiveSlackEventUseCase,
            SlackSignatureVerifier slackSignatureVerifier) {
        this.receiveSlackEventUseCase = receiveSlackEventUseCase;
        this.slackSignatureVerifier = slackSignatureVerifier;
    }

    @PostMapping("/events")
    public ResponseEntity<Object> receiveSlackEvent(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @RequestHeader(name = "X-Slack-Request-Timestamp", required = false) String slackRequestTimestamp,
            @RequestHeader(name = "X-Slack-Signature", required = false) String slackSignature,
            @RequestBody String rawBody) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);
        SlackEventRequest request = parseVerifiedRequest(slackRequestTimestamp, slackSignature, rawBody);

        try {
            ReceiveSlackEventResult result = receiveSlackEventUseCase.receiveSlackEvent(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            if (result.challenge() != null) {
                // Slack URL verification은 RsData envelope 없이 challenge JSON을 그대로 반환해야 한다.
                return ResponseEntity.ok(Map.of("challenge", result.challenge()));
            }
            return ResponseEntity.ok(RsData.ok(SlackEventResponse.from(result)));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private SlackEventRequest parseVerifiedRequest(String timestamp, String signature, String rawBody) {
        try {
            slackSignatureVerifier.verifyOrThrow(timestamp, signature, rawBody);
            return objectMapper.readValue(rawBody, SlackEventRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String requireRequesterGoogleSub(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return requesterGoogleSub.trim();
    }
}
