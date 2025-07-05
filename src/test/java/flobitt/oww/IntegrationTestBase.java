package flobitt.oww;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class IntegrationTestBase {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.6")
            .withDatabaseName("oww_test")
            .withUsername("test")
            .withPassword("test");

    static {
        mariaDB.start();
        System.setProperty("spring.datasource.url", mariaDB.getJdbcUrl());
        System.setProperty("spring.datasource.username", mariaDB.getUsername());
        System.setProperty("spring.datasource.password", mariaDB.getPassword());
    }
}