package com.aquilaoffice.application.port.out;

public interface UpdateTaskExecutionPort {

    SavedTaskExecution updateStatus(TaskExecutionStatusUpdate update);
}
