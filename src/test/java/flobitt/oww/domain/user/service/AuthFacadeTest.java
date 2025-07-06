package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.entity.EmailVerificationTest;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RecordApplicationEvents
class AuthFacadeTest extends IntegrationTestBase {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("회원가입 성공 시 사용자와 이메일 인증 정보가 저장되고 이벤트가 발행된다")
    void signUp_Success() {
        // given
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("Test123!@#")
                .build();

        // when
        authFacade.signUp(request);

        // then
        // 1. 사용자가 저장되었는지 확인
        Optional<User> savedUser = userRepository.findByEmailAndIsDeletedFalse("test@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUserLoginId()).isEqualTo("testuser");
        assertThat(savedUser.get().getUserStatus()).isEqualTo(UserStatus.NOT_VERIFIED);
        assertThat(savedUser.get().getEmailVerifiedAt()).isNull();

        // 2. 이메일 인증 정보가 저장되었는지 확인
        Optional<EmailVerificationTest> verification = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        savedUser.get(), VerificationType.SIGNUP, LocalDateTime.now().plusHours(1));
        assertThat(verification).isPresent();
        assertThat(verification.get().getEmail()).isEqualTo("test@example.com");
        assertThat(verification.get().getVerificationType()).isEqualTo(VerificationType.SIGNUP);

        // 3. 이메일 발송 이벤트가 발행되었는지 확인
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
    void signUp_DuplicateEmail_ThrowsException() {
        // given
        User existingUser = createTestUser("existing@example.com", "existinguser");
        userRepository.save(existingUser);

        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("newuser")
                .email("existing@example.com")  // 중복 이메일
                .password("Test123!@#")
                .build();

        // when & then
        assertThatThrownBy(() -> authFacade.signUp(request))
                .isInstanceOf(Exception.class);  // DataIntegrityViolationException 또는 Custom Exception
    }

    @Test
    @DisplayName("이메일 인증 성공 시 사용자 상태가 ACTIVE로 변경된다")
    void verifyEmail_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(
                user.getId(), user.getEmail(), VerificationType.SIGNUP);

        EmailVerificationTest verification = createTestEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        // when
        authFacade.verifyEmail(token);

        // then
        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(verifiedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(verifiedUser.getEmailVerifiedAt()).isNotNull();

        EmailVerificationTest updatedVerification = emailVerificationRepository
                .findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("만료된 토큰으로 이메일 인증 시 예외가 발생한다")
    void verifyEmail_ExpiredToken_ThrowsException() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        // 만료된 토큰 생성 (과거 시간으로 설정)
        EmailVerificationTest expiredVerification = EmailVerificationTest.builder()
                .verificationToken("expired-token")
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().minusHours(1))  // 이미 만료됨
                .user(user)
                .build();
        emailVerificationRepository.save(expiredVerification);

        // when & then
        assertThatThrownBy(() -> authFacade.verifyEmail("expired-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않거나 만료된");
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰으로 인증 시 예외가 발생한다")
    void verifyEmail_DeletedUser_ThrowsException() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        user.delete();  // 사용자 삭제 처리
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(
                user.getId(), user.getEmail(), VerificationType.SIGNUP);

        EmailVerificationTest verification = createTestEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        // when & then
        assertThatThrownBy(() -> authFacade.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않거나 만료된");
    }

    @Test
    @DisplayName("인증 이메일 재발송 성공")
    void resendEmail_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(
                user.getId(), user.getEmail(), VerificationType.SIGNUP);

        EmailVerificationTest verification = createTestEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        ResendEmailReq request = ResendEmailReq.builder()
                .email("test@example.com")
                .type(VerificationType.SIGNUP)
                .build();

        // when
        authFacade.resendEmail(request);

        // then
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 재발송 요청 시 예외가 발생한다")
    void resendEmail_UserNotFound_ThrowsException() {
        // given
        ResendEmailReq request = ResendEmailReq.builder()
                .email("nonexistent@example.com")
                .type(VerificationType.SIGNUP)
                .build();

        // when & then
        assertThatThrownBy(() -> authFacade.resendEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효한 회원 정보가 없습니다");
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

    private EmailVerificationTest createTestEmailVerification(User user, String token) {
        return EmailVerificationTest.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();
    }
}