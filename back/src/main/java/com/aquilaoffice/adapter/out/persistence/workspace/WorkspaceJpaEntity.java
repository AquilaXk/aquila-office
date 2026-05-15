package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
public class WorkspaceJpaEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private MemberJpaEntity createdByMember;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected WorkspaceJpaEntity() {}

    private WorkspaceJpaEntity(
            String name, String description, MemberJpaEntity createdByMember, LocalDateTime deletedAt) {
        this.name = name;
        this.description = description;
        this.createdByMember = createdByMember;
        this.deletedAt = deletedAt;
    }

    public static WorkspaceJpaEntity from(Workspace workspace, MemberJpaEntity createdByMember) {
        return new WorkspaceJpaEntity(
                workspace.getName(), workspace.getDescription(), createdByMember, workspace.getDeletedAt());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MemberJpaEntity getCreatedByMember() {
        return createdByMember;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
