package com.aquilaoffice.global.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공통 BaseEntity의 ID 자동 생성 계약을 실제 JPA 저장 흐름으로 검증한다.
 */
@DataJpaTest
class BaseEntityRepositoryTest {

    @Autowired
    private TestEntityRepository repository;

    @Test
    void persistEntityWithGeneratedId() {
        TestEntity saved = repository.save(new TestEntity("office"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).hasValueSatisfying(found ->
                assertThat(found.name).isEqualTo("office"));
    }

}

/**
 * BaseEntity 상속 매핑을 확인하기 위한 테스트 전용 JPA entity다.
 */
@Entity(name = "base_entity_repository_test_entity")
class TestEntity extends BaseEntity {

    String name;

    protected TestEntity() {}

    TestEntity(String name) {
        this.name = name;
    }
}

/**
 * 테스트 전용 entity를 저장하기 위한 Spring Data repository다.
 */
interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
