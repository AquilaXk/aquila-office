package com.aquilaoffice.domain.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrchestrationSessionTest {

    @Test
    void createSessionNormalizesStepsAndKeepsDependsOnOrder() {
        OrchestrationSession session = OrchestrationSession.create(
                101L,
                301L,
                " google-1 ",
                List.of(
                        OrchestrationStep.create(" plan ", "계획 정리", 501L, List.of()),
                        OrchestrationStep.create(" backend ", "서비스 구현", 502L, List.of("plan"))));

        assertThat(session.getWorkspaceId()).isEqualTo(101L);
        assertThat(session.getSourceTaskId()).isEqualTo(301L);
        assertThat(session.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(session.getStatus()).isEqualTo(OrchestrationSessionStatus.PLANNED);
        assertThat(session.getSteps())
                .extracting(OrchestrationStep::getStepKey)
                .containsExactly("plan", "backend");
        assertThat(session.getSteps().get(1).getDependsOn()).containsExactly("plan");
    }

    @Test
    void createSessionRejectsUnknownDependency() {
        assertThatThrownBy(() -> OrchestrationSession.create(
                        101L,
                        301L,
                        "google-1",
                        List.of(OrchestrationStep.create("backend", "서비스 구현", 502L, List.of("plan")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orchestration step dependency not found");
    }

    @Test
    void readyStepsOnlyIncludePendingStepsWithCompletedDependencies() {
        OrchestrationSession session = OrchestrationSession.restore(
                901L,
                101L,
                301L,
                "google-1",
                OrchestrationSessionStatus.RUNNING,
                List.of(
                        OrchestrationStep.restore(
                                1001L,
                                "plan",
                                "계획 정리",
                                501L,
                                401L,
                                List.of(),
                                OrchestrationStepStatus.SUCCEEDED),
                        OrchestrationStep.restore(
                                1002L,
                                "backend",
                                "서비스 구현",
                                502L,
                                null,
                                List.of("plan"),
                                OrchestrationStepStatus.PENDING),
                        OrchestrationStep.restore(
                                1003L,
                                "qa",
                                "검증",
                                503L,
                                null,
                                List.of("backend"),
                                OrchestrationStepStatus.PENDING)));

        assertThat(session.readySteps())
                .extracting(OrchestrationStep::getStepKey)
                .containsExactly("backend");
    }
}
