package com.aquilaoffice.application.service;

/**
 * 핵심 업무 흐름의 운영 metric을 application service에서 기록하기 위한 경계다.
 */
public interface WorkflowMetrics {

    OperationSample startTaskExecution();

    OperationSample startGatewayRpc(String method);

    void recordSlackEventReceived(String route);

    void recordSlackSignatureFailure(String route);

    void recordSlackDedupeHit();

    void recordSlackProcessingFailure(String stage);

    static WorkflowMetrics noop() {
        return NoopWorkflowMetrics.INSTANCE;
    }

    interface OperationSample {

        void recordSuccess();

        void recordFailure();

        void recordTimeout();

        static OperationSample noop() {
            return NoopOperationSample.INSTANCE;
        }
    }

    enum NoopOperationSample implements OperationSample {
        INSTANCE;

        @Override
        public void recordSuccess() {
        }

        @Override
        public void recordFailure() {
        }

        @Override
        public void recordTimeout() {
        }
    }

    enum NoopWorkflowMetrics implements WorkflowMetrics {
        INSTANCE;

        @Override
        public OperationSample startTaskExecution() {
            return OperationSample.noop();
        }

        @Override
        public OperationSample startGatewayRpc(String method) {
            return OperationSample.noop();
        }

        @Override
        public void recordSlackEventReceived(String route) {
        }

        @Override
        public void recordSlackSignatureFailure(String route) {
        }

        @Override
        public void recordSlackDedupeHit() {
        }

        @Override
        public void recordSlackProcessingFailure(String stage) {
        }
    }
}
