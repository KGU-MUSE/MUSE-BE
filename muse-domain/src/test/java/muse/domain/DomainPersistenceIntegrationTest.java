package muse.domain;

import jakarta.persistence.EntityManager;
import muse.domain.config.DomainJpaConfig;
import muse.domain.support.DomainProbe;
import muse.domain.support.DomainProbeRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = DomainPersistenceIntegrationTest.TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DomainPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired DomainProbeRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void domainConfigurationDiscoversEntitiesAndRepositoriesAndPersistsToPostgres() {
        var saved = repository.saveAndFlush(new DomainProbe("MUSE"));
        entityManager.clear();
        assertThat(repository.findById(saved.getId())).get()
                .extracting(DomainProbe::getName).isEqualTo("MUSE");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(DomainJpaConfig.class)
    static class TestApplication {}
}
