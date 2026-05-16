package com.aquilaoffice.adapter.out.persistence.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * GitHub 자격 증명 persistence adapter가 outbound port 계약대로 저장, 조회, 수정, 삭제를 수행하는지 검증한다.
 */
@DataJpaTest
@Import(GithubCredentialPersistenceAdapter.class)
class GithubCredentialPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private GithubCredentialJpaRepository githubCredentialJpaRepository;

    @Autowired
    private SaveGithubCredentialPort saveGithubCredentialPort;

    @Autowired
    private LoadGithubCredentialsPort loadGithubCredentialsPort;

    @Autowired
    private LoadGithubCredentialPort loadGithubCredentialPort;

    @Autowired
    private ExistsGithubCredentialPort existsGithubCredentialPort;

    @Autowired
    private UpdateGithubCredentialPort updateGithubCredentialPort;

    @Autowired
    private DeleteGithubCredentialPort deleteGithubCredentialPort;

    @Test
    void savePersistsCredentialAndListReturnsMaskedDomainValues() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        GithubCredential savedCredential = saveGithubCredentialPort.save(GithubCredential.create(
                workspaceEntity.getId(), " Main Token ", " ghp_1234567890abcdef ", " creator-google "));

        assertThat(savedCredential.getId()).isNotNull();
        assertThat(savedCredential.getDisplayName()).isEqualTo("Main Token");
        assertThat(loadGithubCredentialsPort.findActiveByWorkspaceId(workspaceEntity.getId()))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(savedCredential.getId());
                    assertThat(found.getWorkspaceId()).isEqualTo(workspaceEntity.getId());
                    assertThat(found.maskedToken()).isEqualTo("ghp_****cdef");
                });
    }

    @Test
    void saveRejectsMissingWorkspaceEntity() {
        assertThatThrownBy(() -> saveGithubCredentialPort.save(GithubCredential.create(
                        404L, "Main Token", "ghp_1234567890abcdef", "creator-google")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
        assertThat(githubCredentialJpaRepository.count()).isZero();
    }

    @Test
    void loadAndUpdateCredentialByIdAndWorkspace() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        GithubCredential savedCredential = saveGithubCredentialPort.save(GithubCredential.create(
                workspaceEntity.getId(), "Main Token", "ghp_1234567890abcdef", "creator-google"));

        GithubCredential loadedCredential = loadGithubCredentialPort
                .findActiveByIdAndWorkspaceId(savedCredential.getId(), workspaceEntity.getId())
                .orElseThrow();
        loadedCredential.update("Backup Token", "ghp_fedcba0987654321");
        GithubCredential updatedCredential = updateGithubCredentialPort.update(loadedCredential);

        assertThat(updatedCredential.getDisplayName()).isEqualTo("Backup Token");
        assertThat(updatedCredential.maskedToken()).isEqualTo("ghp_****4321");
        assertThat(githubCredentialJpaRepository.findById(savedCredential.getId()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getDisplayName()).isEqualTo("Backup Token");
                    assertThat(entity.getToken()).isEqualTo("ghp_fedcba0987654321");
                });
    }

    @Test
    void deleteSoftDeletesAndDuplicateCheckIgnoresDeletedCredential() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        GithubCredential savedCredential = saveGithubCredentialPort.save(GithubCredential.create(
                workspaceEntity.getId(), "Main Token", "ghp_1234567890abcdef", "creator-google"));

        assertThat(existsGithubCredentialPort.existsActiveDisplayName(workspaceEntity.getId(), "Main Token"))
                .isTrue();

        deleteGithubCredentialPort.delete(savedCredential.getId(), workspaceEntity.getId());

        assertThat(loadGithubCredentialsPort.findActiveByWorkspaceId(workspaceEntity.getId())).isEmpty();
        assertThat(loadGithubCredentialPort.findActiveByIdAndWorkspaceId(savedCredential.getId(), workspaceEntity.getId()))
                .isEmpty();
        assertThat(existsGithubCredentialPort.existsActiveDisplayName(workspaceEntity.getId(), "Main Token"))
                .isFalse();
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
