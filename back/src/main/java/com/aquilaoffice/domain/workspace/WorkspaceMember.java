package com.aquilaoffice.domain.workspace;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;

/**
 * 워크스페이스와 회원 사이의 가입 관계와 워크스페이스 내부 권한을 표현한다.
 */
public class WorkspaceMember {

    private final Workspace workspace;
    private final Member member;
    private WorkspaceMemberRole role;
    private final LocalDateTime joinedAt;

    private WorkspaceMember(Workspace workspace, Member member, WorkspaceMemberRole role, LocalDateTime joinedAt) {
        this.workspace = requireWorkspace(workspace);
        this.member = requireMember(member);
        this.role = requireRole(role);
        this.joinedAt = requireJoinedAt(joinedAt);
    }

    public static WorkspaceMember create(
            Workspace workspace, Member member, WorkspaceMemberRole role, LocalDateTime joinedAt) {
        return new WorkspaceMember(workspace, member, role, joinedAt);
    }

    public void changeRole(WorkspaceMemberRole role) {
        this.role = requireRole(role);
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Member getMember() {
        return member;
    }

    public WorkspaceMemberRole getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    private static Workspace requireWorkspace(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }
        return workspace;
    }

    private static Member requireMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("member must not be null");
        }
        return member;
    }

    private static WorkspaceMemberRole requireRole(WorkspaceMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    private static LocalDateTime requireJoinedAt(LocalDateTime joinedAt) {
        if (joinedAt == null) {
            throw new IllegalArgumentException("joinedAt must not be null");
        }
        return joinedAt;
    }
}
