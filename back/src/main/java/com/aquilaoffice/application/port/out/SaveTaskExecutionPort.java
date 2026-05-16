package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.execution.TaskExecution;

public interface SaveTaskExecutionPort {

    SavedTaskExecution save(TaskExecution execution);
}
