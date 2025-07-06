package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.global.properties.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        userService.createUser(user);

        // then
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("사용자 상태를 ACTIVE로 변경 성공")
    void updateUserStatusActive_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");

        // when
        userService.updateUserStatusActive(user);

        // then
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmailAndIsDeletedFalse_Success() {
        // given
        String email = "test@example.com";
        User user = createTestUser(email, "testuser");
        given(userRepository.findByEmailAndIsDeletedFalse(email))
                .willReturn(Optional.of(user));

        // when
        User foundUser = userService.findByEmailAndIsDeletedFalse(email);

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(email);
        verify(userRepository, times(1)).findByEmailAndIsDeletedFalse(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 사용자 조회 시 예외 발생")
    void findByEmailAndIsDeletedFalse_UserNotFound_ThrowsException() {
        // given
        String email = "nonexistent@example.com";
        given(userRepository.findByEmailAndIsDeletedFalse(email))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findByEmailAndIsDeletedFalse(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효한 회원 정보가 없습니다");
    }

    @Test
    @DisplayName("만료된 미인증 사용자 삭제 처리 성공")
    void deleteExpiredUnverifiedUsers_Success() {
        // given
        given(appProperties.getVerificationTokenExpiry()).willReturn(24);

        User expiredUser1 = createTestUser("user1@example.com", "user1");
        User expiredUser2 = createTestUser("user2@example.com", "user2");

        given(userRepository.findExpiredUnverifiedUsers(
                eq(UserStatus.NOT_VERIFIED), any(LocalDateTime.class)))
                .willReturn(Arrays.asList(expiredUser1, expiredUser2));

        // when
        int deletedCount = userService.deleteExpiredUnverifiedUsers();

        // then
        assertThat(deletedCount).isEqualTo(2);
        assertThat(expiredUser1.isDeleted()).isTrue();
        assertThat(expiredUser2.isDeleted()).isTrue();
        assertThat(expiredUser1.getDeletedAt()).isNotNull();
        assertThat(expiredUser2.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("삭제할 미인증 사용자가 없는 경우")
    void deleteExpiredUnverifiedUsers_NoUsersToDelete() {
        // given
        given(appProperties.getVerificationTokenExpiry()).willReturn(24);
        given(userRepository.findExpiredUnverifiedUsers(
                eq(UserStatus.NOT_VERIFIED), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        int deletedCount = userService.deleteExpiredUnverifiedUsers();

        // then
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("만료된 삭제 사용자 완전 삭제 처리 성공")
    void hardDeleteExpiredDeletedUsers_Success() {
        // given
        given(appProperties.getHardDeleteDays()).willReturn(7);

        User deletedUser1 = createTestUser("deleted1@example.com", "deleted1");
        User deletedUser2 = createTestUser("deleted2@example.com", "deleted2");

        given(userRepository.findExpiredDeletedUsers(any(LocalDateTime.class)))
                .willReturn(Arrays.asList(deletedUser1, deletedUser2));

        // when
        int hardDeletedCount = userService.hardDeleteExpiredDeletedUsers();

        // then
        assertThat(hardDeletedCount).isEqualTo(2);
        verify(userRepository, times(2)).delete(any(User.class));
    }

    @Test
    @DisplayName("완전 삭제할 사용자가 없는 경우")
    void hardDeleteExpiredDeletedUsers_NoUsersToDelete() {
        // given
        given(appProperties.getHardDeleteDays()).willReturn(7);
        given(userRepository.findExpiredDeletedUsers(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        int hardDeletedCount = userService.hardDeleteExpiredDeletedUsers();

        // then
        assertThat(hardDeletedCount).isEqualTo(0);
        verify(userRepository, never()).delete(any(User.class));
    }

    private User createTestUser(String email, String loginId) {
        return User.builder()
                .userLoginId(loginId)
                .email(email)
                .password("encodedPassword")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();
    }
}