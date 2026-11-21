package com.aquilaoffice.application.port.out;

@FunctionalInterface
public interface WorkspaceExecutionLock extends AutoCloseable {

    @Override
    void close();
}
