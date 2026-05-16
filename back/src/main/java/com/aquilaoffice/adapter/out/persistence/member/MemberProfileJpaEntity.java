package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 멤버별 표시 이름과 아바타 설정을 저장하는 JPA entity다.
 */
@Entity
@Table(
        name = "member_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
public class MemberProfileJpaEntity extends BaseEntity {

    static final String DEFAULT_AVATAR_KIND = "mira";
    static final String DEFAULT_SKIN_COLOR = "#f8d4b0";
    static final String DEFAULT_HAIR_COLOR = "#1a1a1a";
    static final String DEFAULT_SHIRT_COLOR = "#2a3a4a";

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private MemberJpaEntity member;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_kind", nullable = false, length = 50)
    private String avatarKind;

    @Column(name = "avatar_skin_color", nullable = false, length = 7)
    private String avatarSkinColor;

    @Column(name = "avatar_hair_color", nullable = false, length = 7)
    private String avatarHairColor;

    @Column(name = "avatar_shirt_color", nullable = false, length = 7)
    private String avatarShirtColor;

    protected MemberProfileJpaEntity() {}

    private MemberProfileJpaEntity(
            MemberJpaEntity member,
            String displayName,
            String avatarKind,
            String avatarSkinColor,
            String avatarHairColor,
            String avatarShirtColor) {
        this.member = member;
        this.displayName = displayName;
        this.avatarKind = avatarKind;
        this.avatarSkinColor = avatarSkinColor;
        this.avatarHairColor = avatarHairColor;
        this.avatarShirtColor = avatarShirtColor;
    }

    static MemberProfileJpaEntity createDefault(MemberJpaEntity member) {
        return new MemberProfileJpaEntity(
                member,
                member.getName(),
                DEFAULT_AVATAR_KIND,
                DEFAULT_SKIN_COLOR,
                DEFAULT_HAIR_COLOR,
                DEFAULT_SHIRT_COLOR);
    }

    void update(String displayName, String avatarKind, String skinColor, String hairColor, String shirtColor) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (avatarKind != null) {
            this.avatarKind = avatarKind;
        }
        if (skinColor != null) {
            this.avatarSkinColor = skinColor;
        }
        if (hairColor != null) {
            this.avatarHairColor = hairColor;
        }
        if (shirtColor != null) {
            this.avatarShirtColor = shirtColor;
        }
    }

    MemberJpaEntity getMember() {
        return member;
    }

    String getDisplayName() {
        return displayName;
    }

    String getAvatarKind() {
        return avatarKind;
    }

    String getAvatarSkinColor() {
        return avatarSkinColor;
    }

    String getAvatarHairColor() {
        return avatarHairColor;
    }

    String getAvatarShirtColor() {
        return avatarShirtColor;
    }
}

