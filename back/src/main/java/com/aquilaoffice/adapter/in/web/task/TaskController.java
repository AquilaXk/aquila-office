package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.adapter.in.web.chat.ChatMessageResponse;
import com.aquilaoffice.application.port.in.CancelTaskCommand;
import com.aquilaoffice.application.port.in.CancelTaskUseCase;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsQuery;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsUseCase;
import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.RecoverStaleTasksCommand;
import com.aquilaoffice.application.port.in.RecoverStaleTasksUseCase;
import com.aquilaoffice.application.port.in.RequestTaskApprovalCommand;
import com.aquilaoffice.application.port.in.RequestTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalCommand;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 Task HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
@Tag(name = "Task", description = "Task 생성, 조회, 실행, 상태 변경 API")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final GetTaskDetailUseCase getTaskDetailUseCase;
    private final UpdateTaskStatusUseCase updateTaskStatusUseCase;
    private final RunTaskUseCase runTaskUseCase;
    private final ListTaskExecutionLogsUseCase listTaskExecutionLogsUseCase;
    private final ListChatTaskMessagesUseCase listChatTaskMessagesUseCase;
    private final CancelTaskUseCase cancelTaskUseCase;
    private final RequestTaskApprovalUseCase requestTaskApprovalUseCase;
    private final ReviewTaskApprovalUseCase reviewTaskApprovalUseCase;
    private final RecoverStaleTasksUseCase recoverStaleTasksUseCase;

    public TaskController(
            CreateTaskUseCase createTaskUseCase,
            ListTasksUseCase listTasksUseCase,
            GetTaskDetailUseCase getTaskDetailUseCase,
            UpdateTaskStatusUseCase updateTaskStatusUseCase,
            RunTaskUseCase runTaskUseCase,
            ListTaskExecutionLogsUseCase listTaskExecutionLogsUseCase,
            ListChatTaskMessagesUseCase listChatTaskMessagesUseCase,
            CancelTaskUseCase cancelTaskUseCase,
            RequestTaskApprovalUseCase requestTaskApprovalUseCase,
            ReviewTaskApprovalUseCase reviewTaskApprovalUseCase,
            RecoverStaleTasksUseCase recoverStaleTasksUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.getTaskDetailUseCase = getTaskDetailUseCase;
        this.updateTaskStatusUseCase = updateTaskStatusUseCase;
        this.runTaskUseCase = runTaskUseCase;
        this.listTaskExecutionLogsUseCase = listTaskExecutionLogsUseCase;
        this.listChatTaskMessagesUseCase = listChatTaskMessagesUseCase;
        this.cancelTaskUseCase = cancelTaskUseCase;
        this.requestTaskApprovalUseCase = requestTaskApprovalUseCase;
        this.reviewTaskApprovalUseCase = reviewTaskApprovalUseCase;
        this.recoverStaleTasksUseCase = recoverStaleTasksUseCase;
    }

    @PostMapping
    @Operation(summary = "워크스페이스 Task를 생성한다")
    public ResponseEntity<RsData<TaskResponse>> createTask(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateTaskRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            CreateTaskResult result =
                    createTaskUseCase.createTask(request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.created(URI.create(
                            "/api/v1/workspaces/" + result.workspaceId() + "/tasks/" + result.taskId()))
                    .body(RsData.ok(TaskResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 워크스페이스/Agent 존재 여부와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/run")
    public ResponseEntity<RsData<RunTaskResponse>> createAndRunTask(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateAndRunTaskRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            CreateTaskResult createdTask =
                    createTaskUseCase.createTask(request.toCreateCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(RunTaskResponse.from(runTaskUseCase.runTask(
                    request.toRunCommand(workspaceId, createdTask.taskId(), normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // 생성과 실행 중 어느 단계에서 실패해도 내부 권한/상태 정책을 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<RsData<List<TaskResponse>>> listTasks(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listTasksUseCase
                    .listTasks(new ListTasksQuery(workspaceId, normalizedRequesterGoogleSub))
                    .stream()
                    .map(TaskResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // Task 목록은 워크스페이스 멤버에게만 노출하므로 실패 사유를 노출하지 않는다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<RsData<TaskResponse>> getTaskDetail(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(TaskResponse.from(getTaskDetailUseCase.getTaskDetail(
                    new GetTaskDetailQuery(workspaceId, taskId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException exception) {
            // Task 상세도 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<RsData<TaskResponse>> updateTaskStatus(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(TaskResponse.from(updateTaskStatusUseCase.updateTaskStatus(
                    request.toCommand(workspaceId, taskId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // 상태 변경 권한이나 전이 실패도 내부 정책 노출을 피한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{taskId}/run")
    public ResponseEntity<RsData<RunTaskResponse>> runTask(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication,
            @RequestBody RunTaskRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(RunTaskResponse.from(runTaskUseCase.runTask(
                    request.toCommand(workspaceId, taskId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // 실행 가능 여부와 권한 실패는 Task 상태 정책 노출을 피하기 위해 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{taskId}/logs")
    public ResponseEntity<RsData<List<TaskExecutionLogResponse>>> listTaskLogs(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listTaskExecutionLogsUseCase
                    .listTaskExecutionLogs(
                            new ListTaskExecutionLogsQuery(workspaceId, taskId, normalizedRequesterGoogleSub))
                    .stream()
                    .map(TaskExecutionLogResponse::from)
                    .toList()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping("/{taskId}/messages")
    public ResponseEntity<RsData<List<ChatMessageResponse>>> listTaskMessages(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listChatTaskMessagesUseCase
                    .listChatTaskMessages(
                            new ListChatTaskMessagesQuery(workspaceId, taskId, normalizedRequesterGoogleSub))
                    .stream()
                    .map(ChatMessageResponse::from)
                    .toList()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/recover-stale")
    @Operation(summary = "기준 시간(분)을 초과해 고착된 실행과 Task를 실패 상태로 복구한다")
    public ResponseEntity<RsData<RecoverStaleTasksResponse>> recoverStaleTasks(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody(required = false) RecoverStaleTasksRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        RecoverStaleTasksCommand command;
        try {
            RecoverStaleTasksRequest safeRequest = request != null ? request : new RecoverStaleTasksRequest(null);
            command = safeRequest.toCommand(workspaceId, normalizedRequesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(
                    RecoverStaleTasksResponse.from(recoverStaleTasksUseCase.recoverStaleTasks(command))));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "진행 중이거나 대기 중인 Task와 활성 실행을 취소한다")
    public ResponseEntity<RsData<TaskResponse>> cancelTask(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        CancelTaskCommand command;
        try {
            command = new CancelTaskCommand(workspaceId, taskId, normalizedRequesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(TaskResponse.from(cancelTaskUseCase.cancelTask(command))));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{taskId}/request-approval")
    @Operation(summary = "Task를 사용자 승인 대기(WAITING_USER) 상태로 전환한다")
    public ResponseEntity<RsData<TaskResponse>> requestTaskApproval(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication,
            @Valid @RequestBody(required = false) RequestTaskApprovalRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        RequestTaskApprovalCommand command;
        try {
            RequestTaskApprovalRequest safeRequest = request != null ? request : new RequestTaskApprovalRequest(null);
            command = safeRequest.toCommand(workspaceId, taskId, normalizedRequesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(
                    TaskResponse.from(requestTaskApprovalUseCase.requestApproval(command))));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping("/{taskId}/review-approval")
    @Operation(summary = "승인 대기 중인 Task를 승인 또는 반려한다")
    public ResponseEntity<RsData<TaskResponse>> reviewTaskApproval(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication,
            @Valid @RequestBody ReviewTaskApprovalRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        ReviewTaskApprovalCommand command;
        try {
            command = request.toCommand(workspaceId, taskId, normalizedRequesterGoogleSub);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(RsData.ok(
                    TaskResponse.from(reviewTaskApprovalUseCase.reviewApproval(command))));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
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
