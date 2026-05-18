package com.aquilaoffice.application.port.in;

import java.util.List;

public interface ListTaskExecutionLogsUseCase {

    List<TaskExecutionLogResult> listTaskExecutionLogs(ListTaskExecutionLogsQuery query);
}
