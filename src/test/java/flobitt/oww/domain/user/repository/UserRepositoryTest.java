package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.slice.RepositoryTestBase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class UserRepositoryTest extends RepositoryTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 삭제되지 않은 사용자 조회 성공")
    void findByEmailAndIsDeletedFalse_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        // when
        Optional<User> foundUser = userRepository.findByEmailAndIsDeletedFalse("test@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 사용자는 조회되지 않음")
    void findByEmailAndIsDeletedFalse_DeletedUserNotFound() {
        // given
        User user = createTestUser("deleted@example.com", "deleteduser");
        user.delete();
        entityManager.persistAndFlush(user);

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

        entityManager.persistAndFlush(user1);

        // when & then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(user2);
        }).isInstanceOf(
                PersistenceException.class);
    }

    @Test
    @DisplayName("중복 로그인 ID 저장 시 예외 발생")
    void save_DuplicateLoginId_ThrowsException() {
        // given
        User user1 = createTestUser("user1@example.com", "duplicateId");
        User user2 = createTestUser("user2@example.com", "duplicateId");

        entityManager.persistAndFlush(user1);

        // when & then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(user2);
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("만료된 미인증 사용자 조회")
    void findExpiredUnverifiedUsers_Success() {
        // given
        User unverifiedUser = createTestUser("unverified@example.com", "unverified");
        User verifiedUser = createTestUser("verified@example.com", "verified");
        verifiedUser.updateUserStatusActive();

        entityManager.persistAndFlush(unverifiedUser);
        entityManager.persistAndFlush(verifiedUser);

        // when - 사용자들이 생성된 이후 시점으로 cutoff 설정
        List<User> expiredUsers = userRepository.findExpiredUnverifiedUsers(
                UserStatus.NOT_VERIFIED, LocalDateTime.now());

        // then - 미인증 사용자만 조회됨
        assertThat(expiredUsers).hasSize(1);
        assertThat(expiredUsers.get(0).getEmail()).isEqualTo("unverified@example.com");
    }

    @Test
    @DisplayName("사용자 Soft Delete 확인")
    void userSoftDelete_Success() {
        // given
        User user = createTestUser("delete@example.com", "deleteuser");
        entityManager.persistAndFlush(user);

        // when
        user.delete();
        entityManager.persistAndFlush(user);

        // then
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(deletedUser.getDeletedAt()).isNotNull();

        // 삭제된 사용자는 findByEmailAndIsDeletedFalse로 조회되지 않음
        Optional<User> foundUser = userRepository.findByEmailAndIsDeletedFalse("delete@example.com");
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

    // 리플렉션을 사용해서 createdAt 설정 (테스트용)
    private void setCreatedAt(User user, LocalDateTime createdAt) {
        try {
            Field createdAtField = user.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(user, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
    }
}