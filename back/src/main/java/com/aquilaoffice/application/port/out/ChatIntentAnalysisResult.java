package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;

public record ChatIntentAnalysisResult(
        String title,
        TaskType taskType,
        TaskPriority priority,
        Boolean createPr) {

    public static ChatIntentAnalysisResult empty() {
        return new ChatIntentAnalysisResult(null, null, null, null);
    }
}
