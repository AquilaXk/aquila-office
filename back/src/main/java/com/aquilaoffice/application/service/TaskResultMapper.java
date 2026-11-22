package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskSummary;

final class TaskResultMapper {

    private TaskResultMapper() {}

    static CreateTaskResult toCreateResult(SavedTask task) {
        return new CreateTaskResult(
                task.taskId(),
                task.workspaceId(),
                task.createdByGoogleSub(),
                task.title(),
                task.description(),
                task.type(),
                task.priority(),
                task.assignedAgentId(),
                task.status(),
                task.createdAt(),
                task.updatedAt());
    }

    static ListTasksResult toListResult(TaskSummary task) {
        return new ListTasksResult(
                task.taskId(),
                task.workspaceId(),
                task.createdByGoogleSub(),
                task.title(),
                task.description(),
                task.type(),
                task.priority(),
                task.assignedAgentId(),
                task.status(),
                task.createdAt(),
                task.updatedAt());
    }

    static GetTaskDetailResult toDetailResult(TaskDetail task) {
        return new GetTaskDetailResult(
                task.taskId(),
                task.workspaceId(),
                task.createdByGoogleSub(),
                task.title(),
                task.description(),
                task.type(),
                task.priority(),
                task.assignedAgentId(),
                task.status(),
                task.createdAt(),
                task.updatedAt());
    }

    static UpdateTaskStatusResult toStatusResult(TaskDetail task) {
        return new UpdateTaskStatusResult(
                task.taskId(),
                task.workspaceId(),
                task.createdByGoogleSub(),
                task.title(),
                task.description(),
                task.type(),
                task.priority(),
                task.assignedAgentId(),
                task.status(),
                task.createdAt(),
                task.updatedAt());
    }
}
