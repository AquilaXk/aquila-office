package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
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

    public SlackEventController(ReceiveSlackEventUseCase receiveSlackEventUseCase) {
        this.receiveSlackEventUseCase = receiveSlackEventUseCase;
    }

    @PostMapping("/events")
    public ResponseEntity<Object> receiveSlackEvent(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @RequestBody SlackEventRequest request) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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

    private String requireRequesterGoogleSub(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return requesterGoogleSub.trim();
    }
}
