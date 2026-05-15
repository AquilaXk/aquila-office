package com.aquilaoffice.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void createUserTrimsRequiredFieldsAndAssignsUserRole() {
        Member member = Member.createUser(" google-1 ", " user@example.com ", " User Name ");

        assertThat(member.getGoogleSub()).isEqualTo("google-1");
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getName()).isEqualTo("User Name");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.isAdmin()).isFalse();
    }

    @Test
    void createAdminAssignsAdminRole() {
        Member member = Member.createAdmin("google-admin", "admin@example.com", "Admin");

        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(member.isAdmin()).isTrue();
    }

    @Test
    void updateProfileFieldsRejectBlankValues() {
        Member member = Member.createUser("google-1", "user@example.com", "User");

        member.updateName(" Updated ");
        member.updateEmail(" updated@example.com ");

        assertThat(member.getName()).isEqualTo("Updated");
        assertThat(member.getEmail()).isEqualTo("updated@example.com");
        assertThatThrownBy(() -> member.updateName(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> member.updateEmail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
    }

    @Test
    void createRejectsBlankRequiredFields() {
        assertThatThrownBy(() -> Member.createUser("", "user@example.com", "User"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("googleSub must not be blank");
        assertThatThrownBy(() -> Member.createUser("google-1", " ", "User"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
        assertThatThrownBy(() -> Member.createUser("google-1", "user@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }
}
