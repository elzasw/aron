package cz.inqool.eas.common.tstutil;

import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.domain.index.DomainIndexedObject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.Base58;

import java.time.Duration;
import java.util.Set;

@SpringBootTest(classes = TestInitializer.class)
@ContextConfiguration(classes = TestConfig.class)
public class TestBase {

    static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:11-alpine")
            .withDatabaseName("test-eas-common-db")
            .withUsername("eas")
            .withPassword("pass123")
            .withReuse(true)
            .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("postgresql_test_container"));

    static final GenericContainer<?> ELASTICSEARCH_CONTAINER = new GenericContainer<>(new ImageFromDockerfile()
            .withDockerfileFromBuilder(
                    builder -> builder
                            .from("docker.elastic.co/elasticsearch/elasticsearch-oss:7.6.2")
                            .run("bin/elasticsearch-plugin", "install", "analysis-icu")
                            .build()
            ))
            .withNetworkAliases("elasticsearch-" + Base58.randomString(6))
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(9200, 9300)
            .waitingFor(new HttpWaitStrategy()
                    .forPort(9200)
                    .withStartupTimeout(Duration.ofMinutes(2)))
            .withReuse(true)
            .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("elasticsearch_test_container"));

    static {
        POSTGRE_SQL_CONTAINER.start();
        ELASTICSEARCH_CONTAINER.start();
    }

    @Autowired
    @Lazy
    private Set<DomainRepository<?, ?, ?, ?, ?>> repositories;


    protected Set<Class<? extends DomainIndexedObject<?, ?>>> getIndexedObjectClasses() {
        return Set.of();
    }

    @BeforeEach
    protected void dropAndCreateElasticIndexed() {
        repositories.stream()
                .filter(repo -> getIndexedObjectClasses().contains(repo.getIndexableType()))
                .forEach(repo -> {
                    if (repo.isIndexInitialized()) {
                        repo.dropIndex();
                    }
                    repo.initIndex();
                });
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver", POSTGRE_SQL_CONTAINER::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.jpa.hibernate.naming.implicit-strategy", () -> "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        registry.add("spring.jpa.hibernate.naming.physical-strategy", () -> "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");

        registry.add("spring.liquibase.changeLog", () -> "classpath:/changelog/dbchangelog-test.eas.xml");
        registry.add("spring.liquibase.drop-first", () -> "true");
    }
}
