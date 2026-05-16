package com.aquilaoffice.domain.task;

/**
 * 워크스페이스에서 Agent에게 맡길 작업의 핵심 상태와 전이 규칙을 관리한다.
 */
public class Task {

    private final Long id;
    private final Long workspaceId;
    private final String createdByGoogleSub;
    private final String title;
    private final String description;
    private final TaskType type;
    private final TaskPriority priority;
    private final Long assignedAgentId;
    private TaskStatus status;

    private Task(
            Long id,
            Long workspaceId,
            String createdByGoogleSub,
            String title,
            String description,
            TaskType type,
            TaskPriority priority,
            Long assignedAgentId,
            TaskStatus status) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.title = requireNotBlank(title, "title");
        this.description = normalizeNullable(description);
        this.type = type == null ? TaskType.OTHER : type;
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
        this.assignedAgentId = normalizeAssignedAgentId(assignedAgentId);
        this.status = status == null ? defaultStatus(this.assignedAgentId) : status;
    }

    public static Task create(
            Long workspaceId,
            String createdByGoogleSub,
            String title,
            String description,
            TaskType type,
            TaskPriority priority,
            Long assignedAgentId) {
        return new Task(null, workspaceId, createdByGoogleSub, title, description, type, priority, assignedAgentId, null);
    }

    public static Task restore(
            Long id,
            Long workspaceId,
            String createdByGoogleSub,
            String title,
            String description,
            TaskType type,
            TaskPriority priority,
            Long assignedAgentId,
            TaskStatus status) {
        return new Task(id, workspaceId, createdByGoogleSub, title, description, type, priority, assignedAgentId, status);
    }

    public void changeStatus(TaskStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status.isTerminal()) {
            throw new IllegalStateException("terminal task status cannot be changed");
        }
        if (!canTransitionTo(nextStatus)) {
            throw new IllegalStateException("invalid task status transition");
        }
        this.status = nextStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Long getAssignedAgentId() {
        return assignedAgentId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    private boolean canTransitionTo(TaskStatus nextStatus) {
        if (status == nextStatus) {
            return true;
        }
        return switch (status) {
            case REQUESTED -> nextStatus == TaskStatus.ASSIGNED
                    || nextStatus == TaskStatus.IN_PROGRESS
                    || nextStatus == TaskStatus.CANCELED;
            case ASSIGNED -> nextStatus == TaskStatus.IN_PROGRESS || nextStatus == TaskStatus.CANCELED;
            case IN_PROGRESS -> nextStatus == TaskStatus.WAITING_USER
                    || nextStatus == TaskStatus.COMPLETED
                    || nextStatus == TaskStatus.FAILED
                    || nextStatus == TaskStatus.CANCELED;
            case WAITING_USER -> nextStatus == TaskStatus.IN_PROGRESS
                    || nextStatus == TaskStatus.COMPLETED
                    || nextStatus == TaskStatus.FAILED
                    || nextStatus == TaskStatus.CANCELED;
            case COMPLETED, FAILED, CANCELED -> false;
        };
    }

    private static TaskStatus defaultStatus(Long assignedAgentId) {
        return assignedAgentId == null ? TaskStatus.REQUESTED : TaskStatus.ASSIGNED;
    }

    private static Long normalizeAssignedAgentId(Long assignedAgentId) {
        if (assignedAgentId == null) {
            return null;
        }
        return requirePositive(assignedAgentId, "assignedAgentId");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
