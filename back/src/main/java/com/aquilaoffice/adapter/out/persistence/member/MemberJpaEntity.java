package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 회원 도메인을 users 테이블에 저장하기 위한 JPA entity다.
 */
@Entity
@Table(name = "users")
public class MemberJpaEntity extends BaseEntity {

    @Column(name = "google_sub", nullable = false, unique = true, length = 100)
    private String googleSub;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    protected MemberJpaEntity() {}

    private MemberJpaEntity(String googleSub, String email, String name, MemberRole role) {
        this.googleSub = googleSub;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static MemberJpaEntity create(String googleSub, String email, String name, MemberRole role) {
        return new MemberJpaEntity(googleSub, email, name, role);
    }

    public void updateGoogleLoginProfile(String email, String name, MemberRole requestedRole) {
        this.email = requireNotBlank(email, "email");
        this.name = requireNotBlank(name, "name");
        if (requestedRole == MemberRole.ADMIN) {
            this.role = MemberRole.ADMIN;
        }
    }

    public Member toDomain() {
        return Member.restore(getId(), googleSub, email, name, role);
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

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
