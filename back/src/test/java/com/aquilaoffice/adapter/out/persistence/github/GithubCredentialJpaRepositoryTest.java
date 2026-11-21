package com.aquilaoffice.adapter.out.persistence.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * GitHub 자격 증명 JPA repository가 workspace 단위 active row 조회와 soft delete 제외를 보장하는지 검증한다.
 */
@DataJpaTest
class GithubCredentialJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private GithubCredentialJpaRepository githubCredentialJpaRepository;

    @Test
    void findActiveByWorkspaceIdReturnsNewestFirstAndExcludesDeletedRows() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        WorkspaceJpaEntity otherWorkspaceEntity = saveWorkspaceEntity("other-google");
        GithubCredentialJpaEntity older = githubCredentialJpaRepository.save(
                githubCredentialEntity(workspaceEntity, "Older"));
        GithubCredentialJpaEntity newer = githubCredentialJpaRepository.save(
                githubCredentialEntity(workspaceEntity, "Newer"));
        GithubCredentialJpaEntity deleted = githubCredentialJpaRepository.save(
                githubCredentialEntity(workspaceEntity, "Deleted"));
        deleted.softDelete(LocalDateTime.of(2026, 5, 16, 10, 0));
        githubCredentialJpaRepository.save(githubCredentialEntity(otherWorkspaceEntity, "Other"));

        assertThat(githubCredentialJpaRepository.findActiveByWorkspaceIdOrderByIdDesc(workspaceEntity.getId()))
                .extracting(GithubCredentialJpaEntity::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(githubCredentialJpaRepository.existsActiveDisplayName(workspaceEntity.getId(), "Deleted"))
                .isFalse();
        assertThat(githubCredentialJpaRepository.findActiveByIdAndWorkspaceId(deleted.getId(), workspaceEntity.getId()))
                .isEmpty();
    }

    private GithubCredential credential(Long workspaceId, String displayName) {
        return GithubCredential.create(workspaceId, displayName, "ghp_1234567890abcdef", "creator-google");
    }

    private GithubCredentialJpaEntity githubCredentialEntity(WorkspaceJpaEntity workspaceEntity, String displayName) {
        return GithubCredentialJpaEntity.from(
                credential(workspaceEntity.getId(), displayName),
                workspaceEntity,
                "stored-token-" + displayName);
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
