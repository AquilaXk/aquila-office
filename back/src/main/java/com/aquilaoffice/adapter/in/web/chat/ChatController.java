package com.aquilaoffice.adapter.in.web.chat;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workspace chat HTTP 요청을 application use case로 연결한다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/chat")
public class ChatController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final ListChatTaskMessagesUseCase listChatTaskMessagesUseCase;
    private final ListChatSessionMessagesUseCase listChatSessionMessagesUseCase;

    public ChatController(
            SendChatMessageUseCase sendChatMessageUseCase,
            ListChatTaskMessagesUseCase listChatTaskMessagesUseCase,
            ListChatSessionMessagesUseCase listChatSessionMessagesUseCase) {
        this.sendChatMessageUseCase = sendChatMessageUseCase;
        this.listChatTaskMessagesUseCase = listChatTaskMessagesUseCase;
        this.listChatSessionMessagesUseCase = listChatSessionMessagesUseCase;
    }

    @PostMapping("/messages")
    public ResponseEntity<RsData<ChatMessageSendResponse>> sendChatMessage(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody ChatMessageSendRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(ChatMessageSendResponse.from(sendChatMessageUseCase.sendChatMessage(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // Chat 요청은 workspace 멤버십과 Agent 준비 상태 실패 사유를 외부에 나누어 노출하지 않는다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/tasks/{taskId}/messages")
    public ResponseEntity<RsData<List<ChatMessageResponse>>> listTaskMessages(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listChatTaskMessagesUseCase
                    .listChatTaskMessages(new ListChatTaskMessagesQuery(
                            workspaceId,
                            taskId,
                            normalizedRequesterGoogleSub))
                    .stream()
                    .map(ChatMessageResponse::from)
                    .toList()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/sessions/{chatSessionId}/messages")
    public ResponseEntity<RsData<ChatSessionMessagesResponse>> listSessionMessages(
            @PathVariable Long workspaceId,
            @PathVariable Long chatSessionId,
            @RequestParam(name = "afterMessageId", required = false) Long afterMessageId,
            @RequestParam(name = "limit", required = false) Integer limit,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(ChatSessionMessagesResponse.from(
                    listChatSessionMessagesUseCase.listChatSessionMessages(new ListChatSessionMessagesQuery(
                            workspaceId,
                            chatSessionId,
                            normalizedRequesterGoogleSub,
                            afterMessageId,
                            limit == null ? 0 : limit)))));
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

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
