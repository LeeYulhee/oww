package flobitt.oww.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("사용자 상태를 ACTIVE로 변경 성공")
    void updateUserStatusActive_Success() {
        // given
        User user = User.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();

        LocalDateTime beforeUpdate = LocalDateTime.now();

        // when
        user.updateUserStatusActive();

        // then
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getEmailVerifiedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("사용자 삭제 처리 성공")
    void delete_Success() {
        // given
        User user = User.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();

        LocalDateTime beforeDelete = LocalDateTime.now();

        // when
        user.delete();

        // then
        assertThat(user.getIsDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getDeletedAt()).isAfterOrEqualTo(beforeDelete);
    }

    @Test
    @DisplayName("사용자 빌더 패턴으로 생성 시 기본값 확인")
    void userBuilder_DefaultValues() {
        // when
        User user = User.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        // then
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.NOT_VERIFIED);
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getIsDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }
}