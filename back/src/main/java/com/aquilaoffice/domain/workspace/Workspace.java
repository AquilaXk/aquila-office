package com.aquilaoffice.domain.workspace;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;

public class Workspace {

    private String name;
    private String description;
    private final Member createdByMember;
    private LocalDateTime deletedAt;

    private Workspace(String name, String description, Member createdByMember) {
        this.name = requireNotBlank(name, "name");
        this.description = normalizeDescription(description);
        this.createdByMember = requireCreatedByMember(createdByMember);
    }

    public static Workspace create(String name, String description, Member createdByMember) {
        return new Workspace(name, description, createdByMember);
    }

    public void update(String name, String description) {
        if (name != null) {
            this.name = requireNotBlank(name, "name");
        }
        this.description = normalizeDescription(description);
    }

    public void softDelete(LocalDateTime deletedAt) {
        LocalDateTime requestedDeletedAt = requireDeletedAt(deletedAt);
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
