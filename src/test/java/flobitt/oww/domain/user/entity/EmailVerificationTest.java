package flobitt.oww.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class EmailVerificationTest {

    @Test
    @DisplayName("이메일 인증 처리 성공")
    void updateEmailVerification_Success() {
        // given
        User user = User.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        EmailVerification verification = EmailVerification.builder()
                .verificationToken("test-token")
                .verificationType(VerificationType.SIGNUP)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();

        LocalDateTime beforeVerification = LocalDateTime.now();

        // when
        verification.updateEmailVerification();

        // then
        assertThat(verification.getVerifiedAt()).isNotNull();
        assertThat(verification.getVerifiedAt()).isAfterOrEqualTo(beforeVerification);
    }

    @Test
    @DisplayName("이메일 인증 객체 생성 시 기본값 확인")
    void emailVerificationBuilder_Success() {
        // given
        User user = User.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

        // when
        EmailVerification verification = EmailVerification.builder()
                .verificationToken("test-token")
                .verificationType(VerificationType.SIGNUP)
                .email("test@example.com")
                .expiresAt(expiryTime)
                .user(user)
                .build();

        // then
        assertThat(verification.getVerificationToken()).isEqualTo("test-token");
        assertThat(verification.getVerificationType()).isEqualTo(VerificationType.SIGNUP);
        assertThat(verification.getEmail()).isEqualTo("test@example.com");
        assertThat(verification.getExpiresAt()).isEqualTo(expiryTime);
        assertThat(verification.getUser()).isEqualTo(user);
        assertThat(verification.getVerifiedAt()).isNull();
    }
}