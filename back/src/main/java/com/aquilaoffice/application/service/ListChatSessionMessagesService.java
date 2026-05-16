package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ChatMessageResult;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesResult;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesUseCase;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedChatMessage;
import com.aquilaoffice.domain.member.Member;
import java.util.List;
import java.util.Objects;

/**
 * Chat session message를 cursor 기반으로 조회한다.
 */
public class ListChatSessionMessagesService implements ListChatSessionMessagesUseCase {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadChatMessagesPort loadChatMessagesPort;
    private final LoadChatSessionPort loadChatSessionPort;

    public ListChatSessionMessagesService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadChatMessagesPort loadChatMessagesPort,
            LoadChatSessionPort loadChatSessionPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadChatMessagesPort =
                Objects.requireNonNull(loadChatMessagesPort, "loadChatMessagesPort must not be null");
        this.loadChatSessionPort =
                Objects.requireNonNull(loadChatSessionPort, "loadChatSessionPort must not be null");
    }

    @Override
    public ListChatSessionMessagesResult listChatSessionMessages(ListChatSessionMessagesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeRequester(query.requesterGoogleSub());
        Member requester = requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requester.getGoogleSub());
        loadChatSessionPort
                .findByWorkspaceIdAndChatSessionId(query.workspaceId(), query.chatSessionId())
                .orElseThrow(() -> new IllegalArgumentException("chat session not found"));

        int limit = normalizeLimit(query.limit());
        List<SavedChatMessage> fetched = loadChatMessagesPort.findByWorkspaceIdAndChatSessionIdAfter(
                query.workspaceId(),
                query.chatSessionId(),
                query.afterMessageId(),
                limit + 1);
        boolean hasMore = fetched.size() > limit;
        List<ChatMessageResult> messages = fetched.stream()
                .limit(limit)
                .map(ChatMessageResultMapper::toResult)
                .toList();
        Long nextCursor = hasMore && !messages.isEmpty() ? messages.get(messages.size() - 1).messageId() : null;
        return new ListChatSessionMessagesResult(query.chatSessionId(), messages, nextCursor, hasMore);
    }

    private Member requireExistingMember(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private String normalizeRequester(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return requesterGoogleSub.trim();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
