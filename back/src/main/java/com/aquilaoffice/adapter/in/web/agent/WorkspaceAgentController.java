package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 Agent HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/agents")
public class WorkspaceAgentController {

    private final CreateWorkspaceAgentUseCase createWorkspaceAgentUseCase;
    private final ListWorkspaceAgentsUseCase listWorkspaceAgentsUseCase;
    private final GetWorkspaceAgentUseCase getWorkspaceAgentUseCase;
    private final DeleteWorkspaceAgentUseCase deleteWorkspaceAgentUseCase;

    public WorkspaceAgentController(
            CreateWorkspaceAgentUseCase createWorkspaceAgentUseCase,
            ListWorkspaceAgentsUseCase listWorkspaceAgentsUseCase,
            GetWorkspaceAgentUseCase getWorkspaceAgentUseCase,
            DeleteWorkspaceAgentUseCase deleteWorkspaceAgentUseCase) {
        this.createWorkspaceAgentUseCase = createWorkspaceAgentUseCase;
        this.listWorkspaceAgentsUseCase = listWorkspaceAgentsUseCase;
        this.getWorkspaceAgentUseCase = getWorkspaceAgentUseCase;
        this.deleteWorkspaceAgentUseCase = deleteWorkspaceAgentUseCase;
    }

    @PostMapping
    public ResponseEntity<RsData<WorkspaceAgentResponse>> createWorkspaceAgent(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @Valid @RequestBody CreateWorkspaceAgentRequest request) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            CreateWorkspaceAgentResult result = createWorkspaceAgentUseCase.createWorkspaceAgent(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.created(URI.create(
                            "/api/v1/workspaces/" + result.workspaceId() + "/agents/" + result.agentId()))
                    .body(RsData.ok(WorkspaceAgentResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // Agent 생성 권한이 없거나 워크스페이스를 찾을 수 없는 경우 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<RsData<List<WorkspaceAgentResponse>>> listWorkspaceAgents(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            return ResponseEntity.ok(RsData.ok(listWorkspaceAgentsUseCase
                    .listWorkspaceAgents(new ListWorkspaceAgentsQuery(workspaceId, normalizedRequesterGoogleSub))
                    .stream()
                    .map(WorkspaceAgentResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // Agent 목록은 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<RsData<WorkspaceAgentResponse>> getWorkspaceAgent(
            @PathVariable Long workspaceId,
            @PathVariable Long agentId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            return ResponseEntity.ok(RsData.ok(WorkspaceAgentResponse.from(
                    getWorkspaceAgentUseCase.getWorkspaceAgent(
                            new GetWorkspaceAgentQuery(workspaceId, agentId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // Agent 상세도 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<RsData<Void>> deleteWorkspaceAgent(
            @PathVariable Long workspaceId,
            @PathVariable Long agentId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            deleteWorkspaceAgentUseCase.deleteWorkspaceAgent(
                    new DeleteWorkspaceAgentCommand(workspaceId, agentId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // 삭제 권한이 없거나 대상 Agent가 없는 경우 같은 404로 숨긴다.
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
