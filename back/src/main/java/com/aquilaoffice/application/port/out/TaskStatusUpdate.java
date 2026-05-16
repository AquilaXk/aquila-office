package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.task.TaskStatus;

public record TaskStatusUpdate(Long workspaceId, Long taskId, TaskStatus status) {}
