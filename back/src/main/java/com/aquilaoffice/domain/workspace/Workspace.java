package com.aquilaoffice.domain.workspace;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;

/**
 * 워크스페이스의 이름, 설명, 생성자, 삭제 상태를 관리하는 순수 도메인 모델이다.
 */
public class Workspace {

    private String name;
    private String description;
    private Integer dailyTokenLimit;
    private final Member createdByMember;
    private LocalDateTime deletedAt;

    private Workspace(String name, String description, Member createdByMember, Integer dailyTokenLimit) {
        this.name = requireNotBlank(name, "name");
        this.description = normalizeDescription(description);
        this.createdByMember = requireCreatedByMember(createdByMember);
        this.dailyTokenLimit = normalizeDailyTokenLimit(dailyTokenLimit);
    }

    public static Workspace create(String name, String description, Member createdByMember) {
        return create(name, description, createdByMember, null);
    }

    public static Workspace create(
            String name,
            String description,
            Member createdByMember,
            Integer dailyTokenLimit) {
        return new Workspace(name, description, createdByMember, dailyTokenLimit);
    }

    public void update(String name, String description) {
        if (name != null) {
            this.name = requireNotBlank(name, "name");
        }
        this.description = normalizeDescription(description);
    }

    public void update(String name, String description, Integer dailyTokenLimit) {
        update(name, description);
        this.dailyTokenLimit = normalizeDailyTokenLimit(dailyTokenLimit);
    }

    public Integer getDailyTokenLimit() {
        return dailyTokenLimit;
    }

    private static Integer normalizeDailyTokenLimit(Integer dailyTokenLimit) {
        if (dailyTokenLimit == null || dailyTokenLimit <= 0) {
            return null;
        }
        return dailyTokenLimit;
    }

    public void softDelete(LocalDateTime deletedAt) {
        LocalDateTime requestedDeletedAt = requireDeletedAt(deletedAt);
        // soft delete는 한 번 기록된 삭제 시각을 보존해 재호출 시에도 삭제 이력이 흔들리지 않게 한다.
        if (this.deletedAt == null) {
            this.deletedAt = requestedDeletedAt;
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Member getCreatedByMember() {
        return createdByMember;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private static Member requireCreatedByMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("createdByMember must not be null");
        }
        return member;
    }

    private static LocalDateTime requireDeletedAt(LocalDateTime deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        return deletedAt;
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
