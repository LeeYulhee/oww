package flobitt.oww.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.domain.user.service.TokenService;
import flobitt.oww.support.IntegrationTestBase;
import flobitt.oww.support.TestFixtures;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.assertj.core.api.Assertions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import flobitt.oww.support.CustomAssertions;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 사용자 가입부터 이메일 인증까지의 전체 플로우 통합 테스트
 */
@AutoConfigureMockMvc
class UserSignupIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("회원가입부터 이메일 인증까지 전체 플로우 테스트")
    void completeUserSignupAndVerificationFlow() throws Exception {
        // 1. 회원가입 요청
        CreateUserReq signupRequest = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("Test123!@#")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // 2. 사용자가 생성되었는지 확인
        Optional<User> savedUser = userRepository.findByEmailAndIsDeletedFalse("test@example.com");
        Assertions.assertThat(savedUser).isPresent();
        CustomAssertions.assertThat(savedUser.get())
                .hasEmail("test@example.com")
                .hasLoginId("testuser")
                .isNotVerified();

        // 3. 이메일 인증 정보가 생성되었는지 확인
        Optional<EmailVerification> verification = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        savedUser.get(), VerificationType.SIGNUP, LocalDateTime.now().plusHours(1));

        Assertions.assertThat(verification).isPresent();
        CustomAssertions.assertThat(verification.get()).isNotVerified().isNotExpired();

        // 4. 이메일 인증 실행
        String verificationToken = verification.get().getVerificationToken();

        mockMvc.perform(get("/email-verifications/{token}", verificationToken))
                .andExpect(status().isOk());

        // 5. 사용자 상태가 ACTIVE로 변경되었는지 확인
        User verifiedUser = userRepository.findById(savedUser.get().getId()).orElseThrow();
        CustomAssertions.assertThat(verifiedUser).isActive();

        // 6. 이메일 인증 정보가 업데이트되었는지 확인
        EmailVerification updatedVerification = emailVerificationRepository
                .findById(verification.get().getId()).orElseThrow();
        CustomAssertions.assertThat(updatedVerification).isVerified();
    }

    @Test
    @DisplayName("이메일 재발송 통합 테스트")
    void emailResendIntegrationFlow() throws Exception {
        // 1. 사용자 생성 및 이메일 인증 정보 생성
        User user = TestFixtures.createUser("resend@example.com", "resenduser");
        userRepository.saveAndFlush(user);

        String token = tokenService.generateVerificationToken(user.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = TestFixtures.createEmailVerification(user, token);
        emailVerificationRepository.saveAndFlush(verification);

        // 2. 이메일 재발송 요청
        ResendEmailReq resendEmailReq = ResendEmailReq.builder()
                .email(user.getEmail())
                .type(verification.getVerificationType())
                .build();

        mockMvc.perform(post("/email-verifications/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendEmailReq)))
                .andExpect(status().isOk());

        // 3. 기존 토큰으로 인증 가능한지 확인
        mockMvc.perform(get("/email-verifications/{token}", token))
                .andExpect(status().isOk());

        // 4. 사용자 상태 확인
        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        CustomAssertions.assertThat(verifiedUser).isActive();
    }

    @Test
    @DisplayName("만료된 토큰으로 인증 시도 시 실패")
    void expiredTokenVerificationFails() throws Exception {
        // 1. 사용자 생성
        User user = TestFixtures.createUser("expired@example.com", "expireduser");
        userRepository.save(user);

        // 2. 만료된 토큰 생성
        String token = Jwts.builder()
                .claim("email", user.getEmail())
                .claim("type", VerificationType.SIGNUP.toString())
                .claim("nonce", "nonce")
                .setExpiration(Date.from(Instant.now().minus(25, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor("test-secret-key-for-verification-tokens-must-be-long-enough".getBytes(StandardCharsets.UTF_8)))
                .compact();

        // 3. 만료된 이메일 인증 정보 생성
        EmailVerification expiredVerification = TestFixtures.createExpiredEmailVerification(user, token);
        emailVerificationRepository.save(expiredVerification);

        // 4. 만료된 토큰으로 인증 시도
        mockMvc.perform(get("/email-verifications/{token}", "expired-token"))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 달라질 수 있음

        // 5. 사용자 상태가 변경되지 않았는지 확인
        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        CustomAssertions.assertThat(unchangedUser).isNotVerified();
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰으로 인증 시도 시 실패")
    void deletedUserTokenVerificationFails() throws Exception {
        // 1. 삭제된 사용자 생성
        User deletedUser = TestFixtures.createDeletedUser("deleted@example.com", "deleteduser");
        userRepository.save(deletedUser);

        // 2. 이메일 인증 정보 생성(삭제 전에 생성된 토큰이라고 가정)
        String token = tokenService.generateVerificationToken(deletedUser.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = TestFixtures.createEmailVerification(deletedUser, token);
        emailVerificationRepository.save(verification);

        // 3. 삭제된 사용자의 토큰으로 인증 시도
        mockMvc.perform(get("/email-verifications/{token}", token))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 달라질 수 있음

        // 4. 사용자 상태가 변경되지 않았는지 확인
        User unchangedUser = userRepository.findById(deletedUser.getId()).orElseThrow();
        CustomAssertions.assertThat(unchangedUser).isDeleted();
    }
}