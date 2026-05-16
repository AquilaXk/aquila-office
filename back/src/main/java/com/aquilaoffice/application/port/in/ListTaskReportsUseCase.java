package com.aquilaoffice.application.port.in;

import java.util.List;

public interface ListTaskReportsUseCase {

    List<TaskReportResult> listTaskReports(ListTaskReportsQuery query);
}
