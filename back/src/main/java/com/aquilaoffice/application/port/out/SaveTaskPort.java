package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.task.Task;

public interface SaveTaskPort {

    SavedTask save(Task task);
}
