package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ChatMessageResult;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import java.util.List;
import java.util.Objects;

/**
 * Task에 연결된 chat message 목록을 workspace 멤버에게 노출한다.
 */
public class ListChatTaskMessagesService implements ListChatTaskMessagesUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadChatMessagesPort loadChatMessagesPort;

    public ListChatTaskMessagesService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadChatMessagesPort loadChatMessagesPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadChatMessagesPort =
                Objects.requireNonNull(loadChatMessagesPort, "loadChatMessagesPort must not be null");
    }

    @Override
    public List<ChatMessageResult> listChatTaskMessages(ListChatTaskMessagesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeRequester(query.requesterGoogleSub());
        Member requester = requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requester.getGoogleSub());
        return loadChatMessagesPort.findByWorkspaceIdAndTaskId(query.workspaceId(), query.taskId()).stream()
                .map(ChatMessageResultMapper::toResult)
                .toList();
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
}
