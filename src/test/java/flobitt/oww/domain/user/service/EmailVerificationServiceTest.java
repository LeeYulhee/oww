package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.*;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppProperties appProperties;

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser("test@example.com", "testuser");
    }

    void setMimeSession() {
        // MimeMessage 모킹을 위한 Session 생성
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mockMimeMessage = new MimeMessage(session);

        given(mailSender.createMimeMessage()).willReturn(mockMimeMessage);
        given(appProperties.getFrontendUrl()).willReturn("http://localhost:3000");
        given(mailProperties.getUsername()).willReturn("noreply@oww.com");
    }

    @Test
    @DisplayName("이메일 인증 정보 생성 및 저장 성공")
    void createEmailVerification_Success() {
        // given
        given(appProperties.getVerificationTokenExpiry()).willReturn(24);
        String token = "test-verification-token";

        // when
        emailVerificationService.createEmailVerification(testUser, token);

        // then
        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository, times(1)).save(captor.capture());

        EmailVerification savedVerification = captor.getValue();
        assertThat(savedVerification.getVerificationToken()).isEqualTo(token);
        assertThat(savedVerification.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(savedVerification.getVerificationType()).isEqualTo(VerificationType.SIGNUP);
        assertThat(savedVerification.getUser()).isEqualTo(testUser);
        assertThat(savedVerification.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("이메일 인증 상태 업데이트 성공")
    void updateEmailVerification_Success() {
        // given
        EmailVerification verification = createTestEmailVerification(testUser, "test-token");

        // when
        emailVerificationService.updateEmailVerification(verification);

        // then
        assertThat(verification.getVerifiedAt()).isNotNull();
        assertThat(verification.getVerifiedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("유효한 인증 토큰 조회 성공")
    void findValidVerificationByParseToken_Success() {
        // given
        String token = "valid-token";
        ParseTokenDto parseTokenDto = ParseTokenDto.builder()
                .email("test@example.com")
                .tokenType("SIGNUP")
                .build();

        EmailVerification verification = createTestEmailVerification(testUser, token);
        given(emailVerificationRepository.findValidVerificationByParseToken(
                eq(parseTokenDto), eq(token), any(LocalDateTime.class)))
                .willReturn(Optional.of(verification));

        // when
        EmailVerification foundVerification = emailVerificationService
                .findValidVerificationByParseToken(parseTokenDto, token, LocalDateTime.now());

        // then
        assertThat(foundVerification).isNotNull();
        assertThat(foundVerification.getVerificationToken()).isEqualTo(token);
        verify(emailVerificationRepository, times(1))
                .findValidVerificationByParseToken(eq(parseTokenDto), eq(token), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("유효하지 않은 인증 토큰 조회 시 예외 발생")
    void findValidVerificationByParseToken_NotFound_ThrowsException() {
        // given
        String token = "invalid-token";
        ParseTokenDto parseTokenDto = ParseTokenDto.builder()
                .email("test@example.com")
                .tokenType("SIGNUP")
                .build();

        given(emailVerificationRepository.findValidVerificationByParseToken(
                eq(parseTokenDto), eq(token), any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationService
                .findValidVerificationByParseToken(parseTokenDto, token, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않거나 만료된 인증 링크입니다");
    }

    @Test
    @DisplayName("사용자와 인증 타입으로 미인증 토큰 조회 성공")
    void findByUserAndVerificationTypeAndVerificationAtIsNull_Success() {
        // given
        EmailVerification verification = createTestEmailVerification(testUser, "test-token");
        given(emailVerificationRepository.findByUserAndVerificationTypeAndVerificationAtIsNull(
                eq(testUser), eq(VerificationType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(verification));

        // when
        EmailVerification foundVerification = emailVerificationService
                .findByUserAndVerificationTypeAndVerificationAtIsNull(testUser, VerificationType.SIGNUP);

        // then
        assertThat(foundVerification).isNotNull();
        assertThat(foundVerification.getUser()).isEqualTo(testUser);
        assertThat(foundVerification.getVerificationType()).isEqualTo(VerificationType.SIGNUP);
    }

    @Test
    @DisplayName("미인증 토큰이 없을 때 예외 발생")
    void findByUserAndVerificationTypeAndVerificationAtIsNull_NotFound_ThrowsException() {
        // given
        given(emailVerificationRepository.findByUserAndVerificationTypeAndVerificationAtIsNull(
                eq(testUser), eq(VerificationType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationService
                .findByUserAndVerificationTypeAndVerificationAtIsNull(testUser, VerificationType.SIGNUP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조회된 토큰이 없습니다");
    }

    @Test
    @DisplayName("이메일 발송 성공")
    void sendEmail_Success() {
        // given
        String email = "test@example.com";
        String token = "verification-token";
        setMimeSession();
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when
        emailVerificationService.sendEmail(email, token);

        // then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    @DisplayName("이메일 발송 실패 시 예외 발생")
    void sendEmail_Failure_ThrowsException() {
        // given
        String email = "test@example.com";
        String token = "verification-token";
        setMimeSession();
        doThrow(new RuntimeException("SMTP server error"))
                .when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendEmail(email, token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 발송에 실패했습니다");
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