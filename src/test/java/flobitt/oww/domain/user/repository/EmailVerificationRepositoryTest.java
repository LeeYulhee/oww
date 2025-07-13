package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.slice.RepositoryTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationRepositoryTest extends RepositoryTestBase {

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    @DisplayName("유효한 토큰으로 이메일 인증 정보 조회 성공")
    void findValidVerificationByParseToken_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        String token = "valid-token-123";
        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, token,
                LocalDateTime.now().plusHours(1)); // 1시간 후 만료
        entityManager.persistAndFlush(verification);

        ParseTokenDto dto = new ParseTokenDto("test@example.com", "SIGNUP");
        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findValidVerificationByParseToken(dto, token, now);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVerificationToken()).isEqualTo(token);
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getVerificationType()).isEqualTo(VerificationType.SIGNUP);
    }

    @Test
    @DisplayName("만료된 토큰은 조회되지 않음")
    void findValidVerificationByParseToken_ExpiredToken() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        String token = "expired-token-123";
        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, token,
                LocalDateTime.now().minusHours(1)); // 1시간 전 만료
        entityManager.persistAndFlush(verification);

        ParseTokenDto dto = new ParseTokenDto("test@example.com", "SIGNUP");
        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findValidVerificationByParseToken(dto, token, now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이미 인증된 토큰은 조회되지 않음")
    void findValidVerificationByParseToken_AlreadyVerified() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        String token = "verified-token-123";
        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, token,
                LocalDateTime.now().plusHours(1));
        verification.updateEmailVerification(); // 이미 인증됨
        entityManager.persistAndFlush(verification);

        ParseTokenDto dto = new ParseTokenDto("test@example.com", "SIGNUP");
        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findValidVerificationByParseToken(dto, token, now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰은 조회되지 않음")
    void findValidVerificationByParseToken_DeletedUser() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        user.delete(); // 사용자 삭제
        entityManager.persistAndFlush(user);

        String token = "deleted-user-token-123";
        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, token,
                LocalDateTime.now().plusHours(1));
        entityManager.persistAndFlush(verification);

        ParseTokenDto dto = new ParseTokenDto("test@example.com", "SIGNUP");
        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findValidVerificationByParseToken(dto, token, now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("잘못된 이메일로는 토큰 조회 실패")
    void findValidVerificationByParseToken_WrongEmail() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        String token = "valid-token-123";
        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, token,
                LocalDateTime.now().plusHours(1));
        entityManager.persistAndFlush(verification);

        ParseTokenDto dto = new ParseTokenDto("wrong@example.com", "SIGNUP"); // 다른 이메일
        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findValidVerificationByParseToken(dto, token, now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자와 인증 타입으로 미인증 토큰 조회 성공")
    void findByUserAndVerificationTypeAndVerificationAtIsNull_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, "token123",
                LocalDateTime.now().plusHours(1));
        entityManager.persistAndFlush(verification);

        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        user, VerificationType.SIGNUP, now);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVerificationType()).isEqualTo(VerificationType.SIGNUP);
        assertThat(result.get().getVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("만료된 토큰은 사용자 + 타입 조회에서도 제외됨")
    void findByUserAndVerificationTypeAndVerificationAtIsNull_ExpiredToken() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, "token123",
                LocalDateTime.now().minusHours(1)); // 이미 만료
        entityManager.persistAndFlush(verification);

        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        user, VerificationType.SIGNUP, now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이미 인증된 토큰은 사용자 + 타입 조회에서 제외됨")
    void findByUserAndVerificationTypeAndVerificationAtIsNull_AlreadyVerified() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        entityManager.persistAndFlush(user);

        EmailVerification verification = createEmailVerification(
                user, "test@example.com", VerificationType.SIGNUP, "token123",
                LocalDateTime.now().plusHours(1));
        verification.updateEmailVerification(); // 이미 인증됨
        entityManager.persistAndFlush(verification);

        LocalDateTime now = LocalDateTime.now();

        // when
        Optional<EmailVerification> result = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        user, VerificationType.SIGNUP, now);

        // then
        assertThat(result).isEmpty();
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

    private EmailVerification createEmailVerification(User user, String email,
                                                      VerificationType type, String token,
                                                      LocalDateTime expiresAt) {
        return EmailVerification.builder()
                .user(user)
                .email(email)
                .verificationType(type)
                .verificationToken(token)
                .expiresAt(expiresAt)
                .build();
    }
}