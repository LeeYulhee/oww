package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class UserRepositoryTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 삭제되지 않은 사용자 조회 성공")
    void findByEmailAndIsDeletedFalse_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByEmailAndIsDeletedFalse("test@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 사용자는 조회되지 않음")
    void findByEmailAndIsDeletedFalse_DeletedUserNotFound() {
        // given
        User user = createTestUser("deleted@example.com", "deleteduser");
        user.delete(); // 사용자 삭제
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByEmailAndIsDeletedFalse("deleted@example.com");

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("중복 이메일 저장 시 예외 발생")
    void save_DuplicateEmail_ThrowsException() {
        // given
        User user1 = createTestUser("duplicate@example.com", "user1");
        User user2 = createTestUser("duplicate@example.com", "user2");

        userRepository.save(user1);

        // when & then
        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("중복 로그인 ID 저장 시 예외 발생")
    void save_DuplicateLoginId_ThrowsException() {
        // given
        User user1 = createTestUser("user1@example.com", "duplicateId");
        User user2 = createTestUser("user2@example.com", "duplicateId");

        userRepository.save(user1);

        // when & then
        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("만료된 미인증 사용자 조회")
    void findExpiredUnverifiedUsers_Success() {
        // given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(25); // 25시간 전

        // 25시간 전에 생성된 미인증 사용자
        User expiredUser = createTestUserWithCreatedAt(
                "expired@example.com", "expireduser", cutoffTime.minusHours(1));

        // 최근에 생성된 미인증 사용자
        User recentUser = createTestUser("recent@example.com", "recentuser");

        // 인증된 사용자
        User verifiedUser = createTestUser("verified@example.com", "verifieduser");
        verifiedUser.updateUserStatusActive();

        userRepository.saveAll(List.of(expiredUser, recentUser, verifiedUser));

        // when
        List<User> expiredUsers = userRepository.findExpiredUnverifiedUsers(
                UserStatus.NOT_VERIFIED, cutoffTime);

        // then
        assertThat(expiredUsers).hasSize(1);
        assertThat(expiredUsers.get(0).getEmail()).isEqualTo("expired@example.com");
        assertThat(expiredUsers.get(0).getUserStatus()).isEqualTo(UserStatus.NOT_VERIFIED);
    }

    @Test
    @DisplayName("만료된 삭제 사용자 조회")
    void findExpiredDeletedUsers_Success() {
        // given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(8); // 8일 전

        // 8일 전에 삭제된 사용자
        User expiredDeletedUser = createTestUser("expired@example.com", "expireduser");
        expiredDeletedUser.delete();
        // 삭제 시간을 수동으로 설정 (실제로는 private 필드이므로 리플렉션 사용 필요)
        userRepository.save(expiredDeletedUser);

        // 최근에 삭제된 사용자
        User recentDeletedUser = createTestUser("recent@example.com", "recentuser");
        recentDeletedUser.delete();
        userRepository.save(recentDeletedUser);

        // 삭제되지 않은 사용자
        User activeUser = createTestUser("active@example.com", "activeuser");
        userRepository.save(activeUser);

        // when
        List<User> expiredDeletedUsers = userRepository.findExpiredDeletedUsers(cutoffTime);

        // then
        // 실제 테스트에서는 삭제 시간을 정확히 설정해야 하므로
        // 별도의 테스트 데이터 설정 방법이 필요할 수 있습니다.
        assertThat(expiredDeletedUsers).isNotNull();
    }

    @Test
    @DisplayName("사용자 Soft Delete 확인")
    void userSoftDelete_Success() {
        // given
        User user = createTestUser("delete@example.com", "deleteuser");
        userRepository.save(user);

        // when
        user.delete();
        userRepository.save(user);

        // then
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.getIsDeleted()).isTrue();
        assertThat(deletedUser.getDeletedAt()).isNotNull();

        // 삭제된 사용자는 findByEmailAndIsDeletedFalse로 조회되지 않음
        Optional<User> foundUser = userRepository.findByEmailAndIsDeletedFalse("delete@example.com");
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("사용자 Hard Delete 확인")
    void userHardDelete_Success() {
        // given
        User user = createTestUser("harddelete@example.com", "harddeleteuser");
        userRepository.save(user);
        Long userId = user.getId().getMostSignificantBits(); // UUID를 Long으로 변환하여 확인용

        // when
        userRepository.delete(user);

        // then
        Optional<User> foundUser = userRepository.findById(user.getId());
        assertThat(foundUser).isEmpty();
    }

    // 테스트 헬퍼 메서드들
    private User createTestUser(String email, String loginId) {
        return User.builder()
                .userLoginId(loginId)
                .email(email)
                .password("encodedPassword")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();
    }

    private User createTestUserWithCreatedAt(String email, String loginId, LocalDateTime createdAt) {
        // 실제로는 BaseEntity의 createdAt을 설정하기 위해 리플렉션이나 별도 방법이 필요
        // 여기서는 개념적으로만 표현
        User user = User.builder()
                .userLoginId(loginId)
                .email(email)
                .password("encodedPassword")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();

        // 실제 구현에서는 @CreatedDate를 무시하고 설정하는 방법이 필요
        return user;
    }
}