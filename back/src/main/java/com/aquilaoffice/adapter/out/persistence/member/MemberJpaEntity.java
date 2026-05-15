package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

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

    public Member toDomain() {
        if (role == MemberRole.ADMIN) {
            return Member.createAdmin(googleSub, email, name);
        }
        return Member.createUser(googleSub, email, name);
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
}
