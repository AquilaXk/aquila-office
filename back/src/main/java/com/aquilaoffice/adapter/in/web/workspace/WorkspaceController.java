package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceCommand;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryUseCase;
import com.aquilaoffice.application.port.in.LeaveWorkspaceCommand;
import com.aquilaoffice.application.port.in.LeaveWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberCommand;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final GetWorkspaceDashboardSummaryUseCase getWorkspaceDashboardSummaryUseCase;
    private final ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;
    private final ListWorkspaceMemberTaskStatsUseCase listWorkspaceMemberTaskStatsUseCase;
    private final ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;
    private final RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;
    private final ExtendWorkspaceInviteUseCase extendWorkspaceInviteUseCase;
    private final UpdateWorkspaceUseCase updateWorkspaceUseCase;
    private final DeleteWorkspaceUseCase deleteWorkspaceUseCase;
    private final ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase;
    private final RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;
    private final LeaveWorkspaceUseCase leaveWorkspaceUseCase;

    public WorkspaceController(
            CreateWorkspaceUseCase createWorkspaceUseCase,
            CreateWorkspaceInviteUseCase createWorkspaceInviteUseCase,
            ListWorkspacesUseCase listWorkspacesUseCase,
            GetWorkspaceDetailUseCase getWorkspaceDetailUseCase,
            GetWorkspaceDashboardSummaryUseCase getWorkspaceDashboardSummaryUseCase,
            ListWorkspaceMembersUseCase listWorkspaceMembersUseCase,
            ListWorkspaceMemberTaskStatsUseCase listWorkspaceMemberTaskStatsUseCase,
            ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase,
            RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase,
            ExtendWorkspaceInviteUseCase extendWorkspaceInviteUseCase,
            UpdateWorkspaceUseCase updateWorkspaceUseCase,
            DeleteWorkspaceUseCase deleteWorkspaceUseCase,
            ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase,
            RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase,
            LeaveWorkspaceUseCase leaveWorkspaceUseCase) {
        this.createWorkspaceUseCase = createWorkspaceUseCase;
        this.createWorkspaceInviteUseCase = createWorkspaceInviteUseCase;
        this.listWorkspacesUseCase = listWorkspacesUseCase;
        this.getWorkspaceDetailUseCase = getWorkspaceDetailUseCase;
        this.getWorkspaceDashboardSummaryUseCase = getWorkspaceDashboardSummaryUseCase;
        this.listWorkspaceMembersUseCase = listWorkspaceMembersUseCase;
        this.listWorkspaceMemberTaskStatsUseCase = listWorkspaceMemberTaskStatsUseCase;
        this.listWorkspaceInvitesUseCase = listWorkspaceInvitesUseCase;
        this.revokeWorkspaceInviteUseCase = revokeWorkspaceInviteUseCase;
        this.extendWorkspaceInviteUseCase = extendWorkspaceInviteUseCase;
        this.updateWorkspaceUseCase = updateWorkspaceUseCase;
        this.deleteWorkspaceUseCase = deleteWorkspaceUseCase;
        this.changeWorkspaceMemberRoleUseCase = changeWorkspaceMemberRoleUseCase;
        this.removeWorkspaceMemberUseCase = removeWorkspaceMemberUseCase;
        this.leaveWorkspaceUseCase = leaveWorkspaceUseCase;
    }

    @GetMapping
    public ResponseEntity<RsData<List<WorkspaceResponse>>> listWorkspaces(
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

        List<ListWorkspacesResult> results =
                listWorkspacesUseCase.listWorkspaces(new ListWorkspacesQuery(normalizedCreatorGoogleSub));

        return ResponseEntity.ok(RsData.ok(results.stream()
                .map(WorkspaceResponse::from)
                .toList()));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<RsData<WorkspaceResponse>> getWorkspaceDetail(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

        try {
            GetWorkspaceDetailResult result = getWorkspaceDetailUseCase.getWorkspaceDetail(
                    new GetWorkspaceDetailQuery(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 존재하지 않거나 가입하지 않은 워크스페이스는 상세 노출 없이 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{workspaceId}/dashboard/summary")
    public ResponseEntity<RsData<WorkspaceDashboardSummaryResponse>> getWorkspaceDashboardSummary(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

        try {
            GetWorkspaceDashboardSummaryResult result = getWorkspaceDashboardSummaryUseCase.getSummary(
                    new GetWorkspaceDashboardSummaryQuery(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceDashboardSummaryResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 대시보드 집계는 가입된 워크스페이스에만 노출하므로 미가입/미존재를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<RsData<List<WorkspaceMemberResponse>>> listWorkspaceMembers(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

        try {
            List<ListWorkspaceMembersResult> results = listWorkspaceMembersUseCase.listWorkspaceMembers(
                    new ListWorkspaceMembersQuery(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(results.stream()
                    .map(WorkspaceMemberResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // 가입하지 않은 워크스페이스와 없는 워크스페이스는 멤버 목록 노출 없이 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{workspaceId}/members/task-stats")
    public ResponseEntity<RsData<List<WorkspaceMemberTaskStatsResponse>>> listWorkspaceMemberTaskStats(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

        try {
            List<ListWorkspaceMemberTaskStatsResult> results =
                    listWorkspaceMemberTaskStatsUseCase.listMemberTaskStats(
                            new ListWorkspaceMemberTaskStatsQuery(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(results.stream()
                    .map(WorkspaceMemberTaskStatsResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // 통계도 가입된 워크스페이스에만 노출하므로 미가입/미존재를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{workspaceId}/invites")
    public ResponseEntity<RsData<List<WorkspaceInviteManagementResponse>>> listWorkspaceInvites(
            @PathVariable Long workspaceId,
            @RequestParam(name = "status", required = false) String status,
            Authentication authentication) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);
        WorkspaceInviteStatus statusFilter = parseInviteStatus(status);

        try {
            List<ListWorkspaceInvitesResult> results = listWorkspaceInvitesUseCase.listWorkspaceInvites(
                    new ListWorkspaceInvitesQuery(workspaceId, normalizedCreatorGoogleSub, statusFilter));
            return ResponseEntity.ok(RsData.ok(results.stream()
                    .map(WorkspaceInviteManagementResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // 초대 목록은 관리자에게만 노출하므로 없는 워크스페이스와 권한 부족을 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{workspaceId}")
    public ResponseEntity<RsData<WorkspaceResponse>> updateWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            UpdateWorkspaceResult result =
                    updateWorkspaceUseCase.updateWorkspace(request.toCommand(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 설정 변경 권한이 없거나 대상 워크스페이스를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<RsData<Void>> deleteWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // 삭제 권한이 없거나 대상 워크스페이스를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<RsData<Void>> changeWorkspaceMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody ChangeWorkspaceMemberRoleRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            changeWorkspaceMemberRoleUseCase.changeWorkspaceMemberRole(
                    request.toCommand(workspaceId, memberId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 멤버 관리 권한이 없거나 대상 멤버를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<RsData<Void>> removeWorkspaceMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            removeWorkspaceMemberUseCase.removeWorkspaceMember(
                    new RemoveWorkspaceMemberCommand(workspaceId, memberId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 멤버 관리 권한이 없거나 대상 멤버를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{workspaceId}/members/me")
    public ResponseEntity<RsData<Void>> leaveWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(workspaceId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 워크스페이스에 속하지 않은 요청자와 없는 워크스페이스는 동일한 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<RsData<WorkspaceResponse>> createWorkspace(
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

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
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceInviteRequest request) {
        String normalizedCreatorGoogleSub = requireAuthenticatedCreatorGoogleSub(authentication);

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

    @DeleteMapping("/{workspaceId}/invites/{inviteId}")
    public ResponseEntity<RsData<Void>> revokeWorkspaceInvite(
            @PathVariable Long workspaceId,
            @PathVariable Long inviteId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        try {
            revokeWorkspaceInviteUseCase.revokeWorkspaceInvite(
                    new RevokeWorkspaceInviteCommand(workspaceId, inviteId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // 취소 대상 초대가 없는 경우에도 워크스페이스/초대 존재 여부를 구분하지 않는다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{workspaceId}/invites/{inviteId}/extend")
    public ResponseEntity<RsData<WorkspaceInviteManagementResponse>> extendWorkspaceInvite(
            @PathVariable Long workspaceId,
            @PathVariable Long inviteId,
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody(required = false) ExtendWorkspaceInviteRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);
        ExtendWorkspaceInviteRequest normalizedRequest =
                request == null ? new ExtendWorkspaceInviteRequest(null) : request;

        try {
            ExtendWorkspaceInviteResult result = extendWorkspaceInviteUseCase.extendWorkspaceInvite(
                    normalizedRequest.toCommand(workspaceId, inviteId, normalizedCreatorGoogleSub));
            return ResponseEntity.ok(RsData.ok(WorkspaceInviteManagementResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 초대/워크스페이스가 없거나 관리자 권한이 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireCreatorGoogleSub(String creatorGoogleSub) {
        if (creatorGoogleSub == null || creatorGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return creatorGoogleSub.trim();
    }

    private String requireAuthenticatedCreatorGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }

    private WorkspaceInviteStatus parseInviteStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return WorkspaceInviteStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
    }
}
