package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    // 실제 인증 어댑터가 연결되기 전까지 웹 요청에서 생성자를 식별하는 임시 경계값이다.
    public static final String CREATOR_GOOGLE_SUB_HEADER = "X-Aquila-Creator-Google-Sub";

    private final CreateWorkspaceUseCase createWorkspaceUseCase;
    private final CreateWorkspaceInviteUseCase createWorkspaceInviteUseCase;
    private final ListWorkspacesUseCase listWorkspacesUseCase;
    private final GetWorkspaceDetailUseCase getWorkspaceDetailUseCase;

    public WorkspaceController(
            CreateWorkspaceUseCase createWorkspaceUseCase,
            CreateWorkspaceInviteUseCase createWorkspaceInviteUseCase,
            ListWorkspacesUseCase listWorkspacesUseCase,
            GetWorkspaceDetailUseCase getWorkspaceDetailUseCase) {
        this.createWorkspaceUseCase = createWorkspaceUseCase;
        this.createWorkspaceInviteUseCase = createWorkspaceInviteUseCase;
        this.listWorkspacesUseCase = listWorkspacesUseCase;
        this.getWorkspaceDetailUseCase = getWorkspaceDetailUseCase;
    }

    @GetMapping
    public ResponseEntity<RsData<List<WorkspaceResponse>>> listWorkspaces(
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        List<ListWorkspacesResult> results =
                listWorkspacesUseCase.listWorkspaces(new ListWorkspacesQuery(normalizedCreatorGoogleSub));

        return ResponseEntity.ok(RsData.ok(results.stream()
                .map(WorkspaceResponse::from)
                .toList()));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<RsData<WorkspaceResponse>> getWorkspaceDetail(
            @PathVariable Long workspaceId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            GetWorkspaceDetailResult result = getWorkspaceDetailUseCase.getWorkspaceDetail(
                    new GetWorkspaceDetailQuery(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 존재하지 않거나 가입하지 않은 워크스페이스는 상세 노출 없이 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<RsData<WorkspaceResponse>> createWorkspace(
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            // inbound adapter는 HTTP 입력을 application command로 바꾸고, 실제 생성 규칙은 use case에 위임한다.
            CreateWorkspaceResult result = createWorkspaceUseCase.createWorkspace(
                    request.toCommand(normalizedCreatorGoogleSub));

            return ResponseEntity
                    .created(URI.create("/api/v1/workspaces/" + result.workspaceId()))
                    .body(RsData.ok(WorkspaceResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 생성자 회원을 찾을 수 없는 경우도 회원 존재 여부를 드러내지 않도록 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{workspaceId}/invites")
    public ResponseEntity<RsData<WorkspaceInviteResponse>> createWorkspaceInvite(
            @PathVariable Long workspaceId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody CreateWorkspaceInviteRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            CreateWorkspaceInviteResult result = createWorkspaceInviteUseCase.createWorkspaceInvite(
                    request.toCommand(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity
                    .created(URI.create(
                            "/api/v1/workspaces/" + result.workspaceId() + "/invites/" + result.inviteId()))
                    .body(RsData.ok(WorkspaceInviteResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 초대 권한이 없거나 대상 워크스페이스를 찾을 수 없는 경우 같은 404로 응답해 멤버십 정보를 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireCreatorGoogleSub(String creatorGoogleSub) {
        if (creatorGoogleSub == null || creatorGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return creatorGoogleSub.trim();
    }
}
