package flobitt.oww.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트 데이터 정리를 위한 유틸리티 클래스
 */
@TestComponent
public class TestDataCleanup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void cleanupAll() {
        // 외래 키 제약조건 때문에 순서가 중요함
        jdbcTemplate.execute("DELETE FROM EMAIL_VERIFICATIONS");
        jdbcTemplate.execute("DELETE FROM USERS");

        // Auto increment 초기화 (필요한 경우)
        jdbcTemplate.execute("ALTER TABLE EMAIL_VERIFICATIONS AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE USERS AUTO_INCREMENT = 1");
    }

    @Transactional
    public void cleanupUsers() {
        jdbcTemplate.execute("DELETE FROM EMAIL_VERIFICATIONS");
        jdbcTemplate.execute("DELETE FROM USERS");
    }
}