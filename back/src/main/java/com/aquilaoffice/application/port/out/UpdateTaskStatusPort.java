package com.aquilaoffice.application.port.out;

public interface UpdateTaskStatusPort {

    TaskDetail updateStatus(TaskStatusUpdate update);
}
