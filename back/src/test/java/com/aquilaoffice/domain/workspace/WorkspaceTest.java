package com.aquilaoffice.domain.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

    @Test
    void createTrimsNameAndDescription() {
        Member creator = Member.createUser("google-1", "user@example.com", "User");

        Workspace workspace = Workspace.create(" Team Workspace ", " Team description ", creator);

        assertThat(workspace.getName()).isEqualTo("Team Workspace");
        assertThat(workspace.getDescription()).isEqualTo("Team description");
        assertThat(workspace.getCreatedByMember()).isSameAs(creator);
        assertThat(workspace.isDeleted()).isFalse();
    }

    @Test
    void updateKeepsNameWhenNullAndClearsBlankDescription() {
        Member creator = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", "Description", creator);

        workspace.update(null, " ");

        assertThat(workspace.getName()).isEqualTo("Team");
        assertThat(workspace.getDescription()).isNull();

        workspace.update(" Updated ", " Next ");

        assertThat(workspace.getName()).isEqualTo("Updated");
        assertThat(workspace.getDescription()).isEqualTo("Next");
    }

    @Test
    void softDeleteKeepsFirstDeletedAt() {
        Member creator = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, creator);
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime secondDeletedAt = firstDeletedAt.plusDays(1);

        workspace.softDelete(firstDeletedAt);
        workspace.softDelete(secondDeletedAt);

        assertThat(workspace.isDeleted()).isTrue();
        assertThat(workspace.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void createAndUpdateRejectInvalidArguments() {
        Member creator = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, creator);

        assertThatThrownBy(() -> Workspace.create(" ", null, creator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> Workspace.create("Team", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdByMember must not be null");
        assertThatThrownBy(() -> workspace.update(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> workspace.softDelete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deletedAt must not be null");
    }
}
