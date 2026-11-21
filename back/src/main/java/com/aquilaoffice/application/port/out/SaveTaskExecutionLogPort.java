package com.aquilaoffice.application.port.out;

public interface SaveTaskExecutionLogPort {

    SavedTaskExecutionLog save(TaskExecutionLogCommand command);
}
