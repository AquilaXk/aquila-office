package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.TaskSummary;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskStatus;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 워크스페이스 멤버별 작업 상태 count와 점수 순위를 계산하는 application service다.
 */
public class ListWorkspaceMemberTaskStatsService implements ListWorkspaceMemberTaskStatsUseCase {

    private static final int FLOW_SCORE_PER_RUNNING_TASK = 36;
    private static final int WAITING_USER_PENALTY_WEIGHT = 50;
    private static final float HEALTH_SCORE_WEIGHT = 0.35f;
    private static final float FLOW_SCORE_WEIGHT = 0.2f;
    private static final float IMPACT_SCORE_WEIGHT = 0.45f;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final LoadTasksPort loadTasksPort;

    public ListWorkspaceMemberTaskStatsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadTasksPort loadTasksPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.loadTasksPort = Objects.requireNonNull(loadTasksPort, "loadTasksPort must not be null");
    }

    @Override
    public List<ListWorkspaceMemberTaskStatsResult> listMemberTaskStats(
            ListWorkspaceMemberTaskStatsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        requireJoinedWorkspace(query.workspaceId(), query.requesterGoogleSub());

        List<ListWorkspaceMembersResult> members =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(query.workspaceId());
        Map<String, MemberTaskCounts> countsByMemberGoogleSub = initializeCounts(members);
        Map<Long, String> agentOwnerByAgentId = findAgentOwnerByAgentId(query.workspaceId());

        for (TaskSummary task : loadTasksPort.findByWorkspaceId(query.workspaceId())) {
            if (task.assignedAgentId() == null) {
                continue;
            }
            String ownerGoogleSub = agentOwnerByAgentId.get(task.assignedAgentId());
            MemberTaskCounts counts = countsByMemberGoogleSub.get(ownerGoogleSub);
            if (counts == null) {
                continue;
            }
            counts.add(task.status());
        }

        List<UnrankedMemberTaskStats> unranked = members.stream()
                .map(member -> toUnrankedStats(member, countsByMemberGoogleSub.get(member.memberGoogleSub())))
                .sorted(memberTaskStatsComparator())
                .toList();

        return IntStream.range(0, unranked.size())
                .mapToObj(index -> unranked.get(index).toResult(index + 1))
                .toList();
    }

    private void requireJoinedWorkspace(Long workspaceId, String requesterGoogleSub) {
        Member requester = loadMemberPort
                .findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
    }

    private Map<String, MemberTaskCounts> initializeCounts(List<ListWorkspaceMembersResult> members) {
        Map<String, MemberTaskCounts> countsByMemberGoogleSub = new HashMap<>();
        for (ListWorkspaceMembersResult member : members) {
            countsByMemberGoogleSub.put(member.memberGoogleSub(), new MemberTaskCounts());
        }
        return countsByMemberGoogleSub;
    }

    private Map<Long, String> findAgentOwnerByAgentId(Long workspaceId) {
        return loadWorkspaceAgentPort.findActiveByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(
                        SavedWorkspaceAgent::agentId,
                        SavedWorkspaceAgent::createdByGoogleSub,
                        (left, right) -> left));
    }

    private UnrankedMemberTaskStats toUnrankedStats(
            ListWorkspaceMembersResult member,
            MemberTaskCounts counts) {
        MemberTaskCounts safeCounts = counts == null ? new MemberTaskCounts() : counts;
        int taskCount = safeCounts.taskCount;
        int healthScore = calculateHealthScore(
                safeCounts.failedTaskCount, safeCounts.waitingUserTaskCount, taskCount);
        int flowScore = clampScore(safeCounts.runningTaskCount * FLOW_SCORE_PER_RUNNING_TASK);
        int impactScore = percentage(safeCounts.completedTaskCount, taskCount);
        int totalScore = Math.round(healthScore * HEALTH_SCORE_WEIGHT
                + flowScore * FLOW_SCORE_WEIGHT
                + impactScore * IMPACT_SCORE_WEIGHT);

        return new UnrankedMemberTaskStats(
                member.memberId(),
                member.name(),
                taskCount,
                safeCounts.completedTaskCount,
                safeCounts.runningTaskCount,
                safeCounts.failedTaskCount,
                safeCounts.waitingUserTaskCount,
                healthScore,
                flowScore,
                impactScore,
                totalScore);
    }

    private Comparator<UnrankedMemberTaskStats> memberTaskStatsComparator() {
        return Comparator.comparingInt(UnrankedMemberTaskStats::totalScore).reversed()
                .thenComparing(Comparator.comparingInt(UnrankedMemberTaskStats::completedTaskCount).reversed())
                .thenComparing(Comparator.comparingInt(UnrankedMemberTaskStats::runningTaskCount).reversed())
                .thenComparingInt(UnrankedMemberTaskStats::failedTaskCount)
                .thenComparing(UnrankedMemberTaskStats::memberName, Comparator.nullsLast(String::compareTo))
                .thenComparing(UnrankedMemberTaskStats::memberId);
    }

    private int calculateHealthScore(int failedTaskCount, int waitingUserTaskCount, int taskCount) {
        int failurePenalty = Math.round(failedTaskCount * 100f / Math.max(taskCount, 1));
        int waitingUserPenalty =
                Math.round(waitingUserTaskCount * WAITING_USER_PENALTY_WEIGHT / (float) Math.max(taskCount, 1));
        return clampScore(100 - failurePenalty - waitingUserPenalty);
    }

    private int percentage(int numerator, int denominator) {
        return Math.round(numerator * 100f / Math.max(denominator, 1));
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static class MemberTaskCounts {
        private int taskCount;
        private int completedTaskCount;
        private int runningTaskCount;
        private int failedTaskCount;
        private int waitingUserTaskCount;

        private void add(TaskStatus status) {
            if (status == null) {
                return;
            }
            taskCount++;
            switch (status) {
                case COMPLETED -> completedTaskCount++;
                case ASSIGNED, IN_PROGRESS -> runningTaskCount++;
                case FAILED -> failedTaskCount++;
                case WAITING_USER -> waitingUserTaskCount++;
                case REQUESTED, CANCELED -> {
                    // 전체 작업 수에는 포함하지만 세부 상태 지표에는 포함하지 않는다.
                }
            }
        }
    }

    private record UnrankedMemberTaskStats(
            Long memberId,
            String memberName,
            int taskCount,
            int completedTaskCount,
            int runningTaskCount,
            int failedTaskCount,
            int waitingUserTaskCount,
            int healthScore,
            int flowScore,
            int impactScore,
            int totalScore) {

        private ListWorkspaceMemberTaskStatsResult toResult(int rank) {
            return new ListWorkspaceMemberTaskStatsResult(
                    memberId,
                    memberName,
                    rank,
                    taskCount,
                    completedTaskCount,
                    runningTaskCount,
                    failedTaskCount,
                    waitingUserTaskCount,
                    healthScore,
                    flowScore,
                    impactScore,
                    totalScore);
        }
    }
}
