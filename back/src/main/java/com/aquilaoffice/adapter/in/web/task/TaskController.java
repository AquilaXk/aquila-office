package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 Task HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final GetTaskDetailUseCase getTaskDetailUseCase;
    private final UpdateTaskStatusUseCase updateTaskStatusUseCase;

    public TaskController(
            CreateTaskUseCase createTaskUseCase,
            ListTasksUseCase listTasksUseCase,
            GetTaskDetailUseCase getTaskDetailUseCase,
            UpdateTaskStatusUseCase updateTaskStatusUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.getTaskDetailUseCase = getTaskDetailUseCase;
        this.updateTaskStatusUseCase = updateTaskStatusUseCase;
    }

    @PostMapping
    public ResponseEntity<RsData<TaskResponse>> createTask(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @Valid @RequestBody CreateTaskRequest request) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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

    @GetMapping
    public ResponseEntity<RsData<List<TaskResponse>>> listTasks(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

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
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            return ResponseEntity.ok(RsData.ok(TaskResponse.from(updateTaskStatusUseCase.updateTaskStatus(
                    request.toCommand(workspaceId, taskId, normalizedRequesterGoogleSub)))));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // 상태 변경 권한이나 전이 실패도 내부 정책 노출을 피한다.
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
