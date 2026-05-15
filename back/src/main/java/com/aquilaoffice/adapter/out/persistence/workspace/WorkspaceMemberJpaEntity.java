package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.domain.workspace.WorkspaceMember;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "workspace_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "member_id"}))
public class WorkspaceMemberJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private WorkspaceMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    protected WorkspaceMemberJpaEntity() {}

    private WorkspaceMemberJpaEntity(
            WorkspaceJpaEntity workspace, MemberJpaEntity member, WorkspaceMemberRole role, LocalDateTime joinedAt) {
        this.workspace = workspace;
        this.member = member;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public static WorkspaceMemberJpaEntity from(
            WorkspaceMember workspaceMember, WorkspaceJpaEntity workspace, MemberJpaEntity member) {
        return new WorkspaceMemberJpaEntity(
                workspace, member, workspaceMember.getRole(), workspaceMember.getJoinedAt());
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public MemberJpaEntity getMember() {
        return member;
    }

    public WorkspaceMemberRole getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
