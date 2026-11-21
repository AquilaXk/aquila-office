package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.ArtifactNodeResult;
import com.aquilaoffice.application.port.in.ArtifactTreeResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanArtifactResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanFileResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanStepResult;
import com.aquilaoffice.application.port.out.ArtifactFileData;
import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.OrchestrationPlanArtifactData;
import com.aquilaoffice.application.port.out.OrchestrationPlanFileData;
import com.aquilaoffice.application.port.out.OrchestrationPlanStepData;

final class ArtifactResultMapper {

    private ArtifactResultMapper() {}

    static ArtifactTreeResult toTreeResult(ArtifactTreeData tree) {
        return new ArtifactTreeResult(
                tree.workspaceId(),
                tree.rootPath(),
                tree.children().stream().map(ArtifactResultMapper::toNodeResult).toList());
    }

    static ArtifactFileResult toFileResult(ArtifactFileData file) {
        return new ArtifactFileResult(
                file.workspaceId(),
                file.path(),
                file.name(),
                file.contentType(),
                file.sizeBytes(),
                file.content());
    }

    static OrchestrationPlanArtifactResult toOrchestrationPlanResult(OrchestrationPlanArtifactData artifact) {
        return new OrchestrationPlanArtifactResult(
                artifact.workspaceId(),
                artifact.planId(),
                artifact.steps().stream().map(ArtifactResultMapper::toOrchestrationPlanStepResult).toList());
    }

    private static ArtifactNodeResult toNodeResult(ArtifactNodeData node) {
        return new ArtifactNodeResult(
                node.name(),
                node.path(),
                node.type().name(),
                node.contentType(),
                node.sizeBytes(),
                node.children().stream().map(ArtifactResultMapper::toNodeResult).toList());
    }

    private static OrchestrationPlanStepResult toOrchestrationPlanStepResult(OrchestrationPlanStepData step) {
        return new OrchestrationPlanStepResult(
                step.stepId(),
                step.stepKey(),
                step.sequenceNo(),
                step.title(),
                step.files().stream().map(ArtifactResultMapper::toOrchestrationPlanFileResult).toList());
    }

    private static OrchestrationPlanFileResult toOrchestrationPlanFileResult(OrchestrationPlanFileData file) {
        return new OrchestrationPlanFileResult(
                file.path(),
                file.name(),
                file.contentType(),
                file.sizeBytes(),
                file.exists());
    }
}
