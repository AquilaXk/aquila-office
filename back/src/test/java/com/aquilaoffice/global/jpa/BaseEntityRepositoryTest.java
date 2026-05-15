package com.aquilaoffice.global.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.JpaRepository;

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

@Entity(name = "base_entity_repository_test_entity")
class TestEntity extends BaseEntity {

    String name;

    protected TestEntity() {}

    TestEntity(String name) {
        this.name = name;
    }
}

interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
