package com.aquilaoffice.domain.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 멤버 도메인 모델의 관계 필수값과 역할 변경 규칙을 검증한다.
 */
class WorkspaceMemberTest {

    @Test
    void createRequiresWorkspaceMemberRoleAndJoinedAt() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, member);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        WorkspaceMember workspaceMember =
                WorkspaceMember.create(workspace, member, WorkspaceMemberRole.ADMIN, joinedAt);

        assertThat(workspaceMember.getWorkspace()).isSameAs(workspace);
        assertThat(workspaceMember.getMember()).isSameAs(member);
        assertThat(workspaceMember.getRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(workspaceMember.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    void changeRoleRejectsNullRole() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, member);
        WorkspaceMember workspaceMember =
                WorkspaceMember.create(workspace, member, WorkspaceMemberRole.MEMBER, LocalDateTime.now());

        workspaceMember.changeRole(WorkspaceMemberRole.ADMIN);

        assertThat(workspaceMember.getRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThatThrownBy(() -> workspaceMember.changeRole(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be null");
    }

    @Test
    void createRejectsMissingArguments() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, member);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        assertThatThrownBy(() -> WorkspaceMember.create(null, member, WorkspaceMemberRole.MEMBER, joinedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace must not be null");
        assertThatThrownBy(() -> WorkspaceMember.create(workspace, null, WorkspaceMemberRole.MEMBER, joinedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member must not be null");
        assertThatThrownBy(() -> WorkspaceMember.create(workspace, member, null, joinedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be null");
        assertThatThrownBy(() -> WorkspaceMember.create(workspace, member, WorkspaceMemberRole.MEMBER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("joinedAt must not be null");
    }
}
