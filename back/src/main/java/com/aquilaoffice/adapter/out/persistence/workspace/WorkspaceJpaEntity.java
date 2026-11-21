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

/**
 * 워크스페이스 도메인을 workspaces 테이블에 저장하기 위한 JPA entity다.
 */
@Entity
@Table(name = "workspaces")
public class WorkspaceJpaEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "daily_token_limit")
    private Integer dailyTokenLimit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private MemberJpaEntity createdByMember;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected WorkspaceJpaEntity() {}

    private WorkspaceJpaEntity(
            String name,
            String description,
            Integer dailyTokenLimit,
            MemberJpaEntity createdByMember,
            LocalDateTime deletedAt) {
        this.name = name;
        this.description = description;
        this.dailyTokenLimit = normalizeDailyTokenLimit(dailyTokenLimit);
        this.createdByMember = createdByMember;
        this.deletedAt = deletedAt;
    }

    public static WorkspaceJpaEntity from(Workspace workspace, MemberJpaEntity createdByMember) {
        // JPA entity는 persistence adapter 내부 모델이므로 domain 객체에서 필요한 값만 복사한다.
        return new WorkspaceJpaEntity(
                workspace.getName(),
                workspace.getDescription(),
                workspace.getDailyTokenLimit(),
                createdByMember,
                workspace.getDeletedAt());
    }

    public void update(String name, String description) {
        this.name = requireNotBlank(name, "name");
        this.description = normalizeDescription(description);
    }

    public void update(String name, String description, Integer dailyTokenLimit) {
        update(name, description);
        this.dailyTokenLimit = normalizeDailyTokenLimit(dailyTokenLimit);
    }

    public void softDelete(LocalDateTime deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        // soft delete는 첫 삭제 시각을 보존해 중복 요청에도 삭제 이력이 바뀌지 않게 한다.
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDailyTokenLimit() {
        return dailyTokenLimit;
    }

    public MemberJpaEntity getCreatedByMember() {
        return createdByMember;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private static Integer normalizeDailyTokenLimit(Integer dailyTokenLimit) {
        if (dailyTokenLimit == null || dailyTokenLimit <= 0) {
            return null;
        }
        return dailyTokenLimit;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
