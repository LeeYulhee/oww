package flobitt.oww.integration;

import flobitt.oww.domain.base.entity.SoftDeleteBaseEntity;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.domain.user.service.UserService;
import flobitt.oww.support.IntegrationTestBase;
import flobitt.oww.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 사용자 정리 스케줄러 통합 테스트
 */
class UserCleanupSchedulerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("만료된 미인증 사용자 삭제 처리 통합 테스트")
    void deleteExpiredUnverifiedUsersIntegration() throws Exception {
        // 1. 테스트 데이터 생성
        // 24시간 이상 된 미인증 사용자
        User expiredUser1 = TestFixtures.createUser("expired1@example.com", "expired1");
        User expiredUser2 = TestFixtures.createUser("expired2@example.com", "expired2");

        // 최근 생성된 미인증 사용자
        User recentUser = TestFixtures.createUser("recent@example.com", "recent");

        // 인증된 사용자
        User verifiedUser = TestFixtures.createVerifiedUser("verified@example.com", "verified");

        userRepository.saveAllAndFlush(List.of(expiredUser1, expiredUser2, recentUser, verifiedUser));

        // 2. 저장 후 생성 시간을 과거로 직접 업데이트(JPA Auditing 우회)
        updateCreatedAt(expiredUser1.getUserLoginId(), LocalDateTime.now().minusHours(25));
        updateCreatedAt(expiredUser2.getUserLoginId(), LocalDateTime.now().minusHours(25));

        // 2. 스케줄러 실행
        int deletedCount = userService.deleteExpiredUnverifiedUsers();

        // 3. 결과 확인
        assertThat(deletedCount).isEqualTo(2);

        // 만료된 사용자들이 삭제 처리되었는지 확인
        User deletedUser1 = userRepository.findById(expiredUser1.getId()).orElseThrow();
        User deletedUser2 = userRepository.findById(expiredUser2.getId()).orElseThrow();
        assertThat(deletedUser1.isDeleted()).isTrue();
        assertThat(deletedUser2.isDeleted()).isTrue();

        // 최근 사용자와 인증된 사용자는 영향받지 않았는지 확인
        User unchangedRecentUser = userRepository.findById(recentUser.getId()).orElseThrow();
        User unchangedVerifiedUser = userRepository.findById(verifiedUser.getId()).orElseThrow();
        assertThat(unchangedRecentUser.isDeleted()).isFalse();
        assertThat(unchangedVerifiedUser.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("만료된 삭제 사용자 완전 삭제 통합 테스트")
    void hardDeleteExpiredDeletedUsersIntegration() throws Exception {
        // 1. 테스트 데이터 생성
        // 7일 이상 전에 삭제된 사용자
        User expiredDeletedUser1 = TestFixtures.createDeletedUser("expired1@example.com", "expired1");
        User expiredDeletedUser2 = TestFixtures.createDeletedUser("expired2@example.com", "expired2");

        // 최근 삭제된 사용자
        User recentDeletedUser = TestFixtures.createDeletedUser("recent@example.com", "recent");

        // 활성 사용자
        User activeUser = TestFixtures.createUser("active@example.com", "active");

        // 삭제 시간을 과거로 설정
        setDeletedAt(expiredDeletedUser1, LocalDateTime.now().minusDays(8));
        setDeletedAt(expiredDeletedUser2, LocalDateTime.now().minusDays(8));

        userRepository.saveAll(List.of(expiredDeletedUser1, expiredDeletedUser2, recentDeletedUser, activeUser));

        // 2. 스케줄러 실행
        int hardDeletedCount = userService.hardDeleteExpiredDeletedUsers();

        // 3. 결과 확인
        assertThat(hardDeletedCount).isEqualTo(2);

        // 4. 완전 삭제된 사용자들이 데이터베이스에서 사라졌는지 확인
        assertThat(userRepository.findById(expiredDeletedUser1.getId())).isEmpty();
        assertThat(userRepository.findById(expiredDeletedUser2.getId())).isEmpty();

        // 5. 최근 삭제된 사용자와 활성 사용자는 여전히 존재하는지 확인
        assertThat(userRepository.findById(recentDeletedUser.getId())).isPresent();
        assertThat(userRepository.findById(activeUser.getId())).isPresent();
    }


    // JPA Auditing을 우회하는 직접 SQL 업데이트 메서드들
    private void updateCreatedAt(String userId, LocalDateTime createdAt) {
        String sql = "UPDATE users SET created_at = ? WHERE user_login_id = ?";
        int updatedRows = jdbcTemplate.update(sql, createdAt, userId);
        System.out.println("Updated created_at for user " + userId + ": " + updatedRows + " rows affected");
    }

    // 리플렉션을 사용한 헬퍼 메서드
    private void setDeletedAt(User user, LocalDateTime deletedAt) throws Exception {
        Field field = SoftDeleteBaseEntity.class.getDeclaredField("deletedAt");
        field.setAccessible(true);
        field.set(user, deletedAt);
    }
}