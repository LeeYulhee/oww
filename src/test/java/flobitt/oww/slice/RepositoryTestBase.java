package flobitt.oww.slice;

import flobitt.oww.TestDataCleanup;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository 레이어만 테스트하기 위한 베이스 클래스
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestDataCleanup.class)
public abstract class RepositoryTestBase {

    @Autowired
    protected TestDataCleanup testDataCleanup;

    @AfterEach
    void cleanUp() {
        testDataCleanup.cleanupAll();
    }
}