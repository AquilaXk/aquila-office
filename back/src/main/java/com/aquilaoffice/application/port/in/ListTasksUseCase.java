package com.aquilaoffice.application.port.in;

import java.util.List;

public interface ListTasksUseCase {

    List<ListTasksResult> listTasks(ListTasksQuery query);
}
