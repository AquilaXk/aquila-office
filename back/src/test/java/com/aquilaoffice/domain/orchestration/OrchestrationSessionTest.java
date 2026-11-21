package com.aquilaoffice.domain.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
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
    void createSessionRejectsThreeNodeDependencyCycle() {
        assertThatThrownBy(() -> OrchestrationSession.create(
                        101L,
                        301L,
                        "google-1",
                        List.of(
                                OrchestrationStep.create("a", "Step A", 501L, List.of("b")),
                                OrchestrationStep.create("b", "Step B", 502L, List.of("c")),
                                OrchestrationStep.create("c", "Step C", 503L, List.of("a")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orchestration step dependencies must not contain cycles");
    }

    @Test
    void createSessionAllowsDiamondDagWithoutCycles() {
        OrchestrationSession session = OrchestrationSession.create(
                101L,
                301L,
                "google-1",
                List.of(
                        OrchestrationStep.create("plan", "Plan", 501L, List.of()),
                        OrchestrationStep.create("api", "API", 502L, List.of("plan")),
                        OrchestrationStep.create("ui", "UI", 503L, List.of("plan")),
                        OrchestrationStep.create("verify", "Verify", 504L, List.of("api", "ui"))));

        assertThat(session.getSteps()).hasSize(4);
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

    @Test
    void createSessionRejectsExceedingMaxStepsAndStepLengthBounds() {
        List<OrchestrationStep> tooManySteps = new ArrayList<>();
        for (int i = 1; i <= OrchestrationSession.MAX_STEPS + 1; i++) {
            tooManySteps.add(OrchestrationStep.create("step-" + i, "지시 " + i, 501L, List.of()));
        }

        assertThatThrownBy(() -> OrchestrationSession.create(101L, 301L, "google-1", tooManySteps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orchestration steps must not exceed 20");

        String longKey = "k".repeat(OrchestrationStep.MAX_STEP_KEY_LENGTH + 1);
        assertThatThrownBy(() -> OrchestrationStep.create(longKey, "지시", 501L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stepKey must not exceed 80 characters");

        String longInstruction = "i".repeat(OrchestrationStep.MAX_INSTRUCTION_LENGTH + 1);
        assertThatThrownBy(() -> OrchestrationStep.create("plan", longInstruction, 501L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instruction must not exceed 1000 characters");

        assertThatThrownBy(() -> OrchestrationStep.create("a\nb", "지시", 501L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stepKey must contain only letters, digits, hyphens, or underscores");

        assertThatThrownBy(() -> OrchestrationStep.create("impl", "지시", 501L, List.of("a\nb")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependsOn must contain only letters, digits, hyphens, or underscores");

        List<String> oversizedDependsOn = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            String prefix = String.format("s%02d_", i);
            oversizedDependsOn.add(prefix + "x".repeat(OrchestrationStep.MAX_STEP_KEY_LENGTH - prefix.length()));
        }
        assertThatThrownBy(() -> OrchestrationStep.create("join", "지시", 501L, oversizedDependsOn))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependsOn serialized length must not exceed 1000 characters");
    }
}
