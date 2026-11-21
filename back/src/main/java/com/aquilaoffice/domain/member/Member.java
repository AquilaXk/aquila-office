package com.aquilaoffice.domain.member;

/**
 * 회원의 식별자와 기본 권한을 관리하는 순수 도메인 모델이다.
 */
public class Member {

    private final Long id;
    private final String googleSub;
    private String email;
    private String name;
    private MemberRole role;

    private Member(Long id, String googleSub, String email, String name, MemberRole role) {
        this.id = id;
        this.googleSub = requireNotBlank(googleSub, "googleSub");
        this.email = requireNotBlank(email, "email");
        this.name = requireNotBlank(name, "name");
        this.role = requireRole(role);
    }

    public static Member createUser(String googleSub, String email, String name) {
        return new Member(null, googleSub, email, name, MemberRole.USER);
    }

    public static Member createAdmin(String googleSub, String email, String name) {
        return new Member(null, googleSub, email, name, MemberRole.ADMIN);
    }

    public static Member restore(Long id, String googleSub, String email, String name, MemberRole role) {
        return new Member(id, googleSub, email, name, role);
    }

    public void updateName(String name) {
        this.name = requireNotBlank(name, "name");
    }

    public void updateEmail(String email) {
        this.email = requireNotBlank(email, "email");
    }

    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MemberRole getRole() {
        return role;
    }

    private static MemberRole requireRole(MemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
