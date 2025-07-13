package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.entity.*;
import flobitt.oww.domain.user.event.ResendVerificationEmailEvent;
import flobitt.oww.domain.user.event.SendVerificationEmailEvent;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.support.IntegrationTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@RecordApplicationEvents
class AuthFacadeTest extends IntegrationTestBase {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

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
        // 1. 사용자가 실제로 DB에 저장되었는지 확인
        Optional<User> savedUser = userRepository.findByEmailAndIsDeletedFalse("test@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUserLoginId()).isEqualTo("testuser");
        assertThat(savedUser.get().getUserStatus()).isEqualTo(UserStatus.NOT_VERIFIED);
        assertThat(savedUser.get().getEmailVerifiedAt()).isNull();

        // 2. 이메일 인증 정보가 실제로 DB에 저장되었는지 확인
        List<EmailVerification> verifications = emailVerificationRepository
                .findAll().stream()
                .filter(v -> v.getUser().equals(savedUser.get()))
                .filter(v -> v.getVerificationType() == VerificationType.SIGNUP)
                .filter(v -> v.getVerifiedAt() == null)
                .toList();

        assertThat(verifications).hasSize(1);
        EmailVerification verification = verifications.getFirst();
        assertThat(verification.getEmail()).isEqualTo("test@example.com");
        assertThat(verification.getVerificationType()).isEqualTo(VerificationType.SIGNUP);
        assertThat(verification.getExpiresAt()).isAfter(LocalDateTime.now());

        // 3. 토큰이 제대로 생성되었는지 확인 (실질적인 비즈니스 로직 검증)
        assertThat(verification.getVerificationToken()).isNotNull();
        assertThat(verification.getVerificationToken()).isNotEmpty();

        assertThat(applicationEvents.stream(SendVerificationEmailEvent.class))
                .hasSize(1);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        // 중복으로 인해 실패했으므로 새로운 사용자는 저장되지 않아야 함
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("existing@example.com"))
                .toList();
        assertThat(users).hasSize(1);  // 기존 사용자만 있어야 함

        // EmailVerification도 생성되지 않아야 함(중복 사용자만 signup()으로 접근하기 때문에 EmailVerification이 없음)
        List<EmailVerification> verifications = emailVerificationRepository.findAll().stream()
                .filter(v -> v.getEmail().equals("existing@example.com"))
                .toList();
        assertThat(verifications).isEmpty();
    }


    @Test
    @DisplayName("중복된 로그인 ID로 회원가입 시 예외가 발생한다")
    void signUp_DuplicateLoginId_ThrowsException() {
        // given
        User existingUser = createTestUser("existing@example.com", "duplicateId");
        userRepository.save(existingUser);

        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("duplicateId")  // 중복 로그인 ID
                .email("new@example.com")
                .password("Test123!@#")
                .build();

        // when & then
        assertThatThrownBy(() -> authFacade.signUp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 ID입니다.");

        // 중복으로 인해 실패했으므로 새로운 사용자는 저장되지 않아야 함
        Optional<User> newUser = userRepository.findByEmailAndIsDeletedFalse("new@example.com");
        assertThat(newUser).isEmpty();
    }

    @Test
    @DisplayName("이메일 인증 성공 시 사용자 상태가 ACTIVE로 변경된다")
    void verifyEmail_Success() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(user.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = createTestEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        // when
        authFacade.verifyEmail(token);

        // then
        // 1. 사용자 상태가 실제로 DB에서 변경되었는지 확인
        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(verifiedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(verifiedUser.getEmailVerifiedAt()).isNotNull();

        // 2. 이메일 인증 정보가 실제로 DB에서 업데이트되었는지 확인
        EmailVerification updatedVerification = emailVerificationRepository
                .findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("만료된 토큰으로 이메일 인증 시 예외가 발생한다 : 토큰 유효시간 만료")
    void verifyEmail_ExpiredToken_ThrowsException() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        String token = Jwts.builder()
                .claim("email", user.getEmail())
                .claim("type", VerificationType.SIGNUP.toString())
                .claim("nonce", "nonce")
                .setExpiration(Date.from(Instant.now().minus(25, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor("test-secret-key-for-verification-tokens-must-be-long-enough".getBytes(StandardCharsets.UTF_8)))
                .compact();

        // 만료된 토큰으로 이메일 인증 정보 생성
        EmailVerification expiredVerification = EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().minusHours(25))  // 이미 만료됨
                .user(user)
                .build();
        emailVerificationRepository.save(expiredVerification);

        // when & then
        assertThatThrownBy(() -> authFacade.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 토큰");
    }

    @Test
    @DisplayName("만료된 토큰으로 이메일 인증 시 예외가 발생한다 : DB 유효시간 만료")
    void verifyEmail_ExpiredTokenWithDB_ThrowsException() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        userRepository.save(user);

        String token = Jwts.builder()
                .claim("email", user.getEmail())
                .claim("type", VerificationType.SIGNUP.toString())
                .claim("nonce", "nonce")
                .setExpiration(Date.from(Instant.now().plus(25, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor("test-secret-key-for-verification-tokens-must-be-long-enough".getBytes(StandardCharsets.UTF_8)))
                .compact();

        // 만료된 토큰으로 이메일 인증 정보 생성
        EmailVerification expiredVerification = EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().minusHours(25))  // 이미 만료됨
                .user(user)
                .build();
        emailVerificationRepository.save(expiredVerification);

        // when & then
        assertThatThrownBy(() -> authFacade.verifyEmail(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않거나 만료된 인증 링크");
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰으로 인증 시 예외가 발생한다")
    void verifyEmail_DeletedUser_ThrowsException() {
        // given
        User user = createTestUser("test@example.com", "testuser");
        user.delete();  // 사용자 삭제 처리
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(user.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = createTestEmailVerification(user, token);
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

        String token = tokenService.generateVerificationToken(user.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = createTestEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        ResendEmailReq request = ResendEmailReq.builder()
                .email("test@example.com")
                .type(VerificationType.SIGNUP)
                .build();

        // when
        authFacade.resendEmail(request);

        // then
        assertThat(applicationEvents.stream(ResendVerificationEmailEvent.class))
                .hasSize(1);
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

    private EmailVerification createTestEmailVerification(User user, String token) {
        return EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();
    }
}