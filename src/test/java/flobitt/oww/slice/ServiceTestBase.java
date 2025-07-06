package flobitt.oww.slice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service 레이어 테스트를 위한 베이스 클래스
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ServiceTestBase {
    // 공통 설정이 필요한 경우 여기에 추가
}