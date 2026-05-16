package com.aquilaoffice.application.port.in;

public interface CreateTaskUseCase {

    CreateTaskResult createTask(CreateTaskCommand command);
}
