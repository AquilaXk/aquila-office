package com.aquilaoffice.application.port.out;

/**
 * 직접 모델 공급자 호출 결과 및 정규화된 토큰 사용량이다.
 */
public record WorkspaceModelRunResult(
        boolean success,
        String finalText,
        String failureReason,
        String resolvedModel,
        Integer promptTokens,
        Integer completionTokens,
        boolean retryable) {

    public static WorkspaceModelRunResult ok(
            String finalText,
            String resolvedModel,
            Integer promptTokens,
            Integer completionTokens) {
        return new WorkspaceModelRunResult(true, finalText, null, resolvedModel, promptTokens, completionTokens, false);
    }

    public static WorkspaceModelRunResult fail(String failureReason, String resolvedModel, boolean retryable) {
        return new WorkspaceModelRunResult(false, null, failureReason, resolvedModel, null, null, retryable);
    }
}
