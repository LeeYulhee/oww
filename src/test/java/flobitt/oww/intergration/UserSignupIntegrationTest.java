package flobitt.oww.intergration;

import com.fasterxml.jackson.databind.ObjectMapper;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.domain.user.service.TokenService;
import flobitt.oww.support.IntegrationTestBase;
import flobitt.oww.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.assertj.core.api.Assertions;

import java.time.LocalDateTime;
import java.util.Optional;

import static flobitt.oww.support.CustomAssertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 사용자 가입부터 이메일 인증까지의 전체 플로우 통합 테스트
 */
@AutoConfigureWebMvc
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
        assertThat(savedUser.get()).hasEmail("test@example.com")
                .hasLoginId("testuser")
                .isNotVerified();

        // 3. 이메일 인증 정보가 생성되었는지 확인
        Optional<EmailVerification> verification = emailVerificationRepository
                .findByUserAndVerificationTypeAndVerificationAtIsNull(
                        savedUser.get(), VerificationType.SIGNUP, LocalDateTime.now().plusHours(1));

        Assertions.assertThat(verification).isPresent();
        assertThat(verification.get()).isNotVerified().isNotExpired();

        // 4. 이메일 인증 실행
        String verificationToken = verification.get().getVerificationToken();

        mockMvc.perform(get("/email-verifications/{token}", verificationToken))
                .andExpect(status().isOk());

        // 5. 사용자 상태가 ACTIVE로 변경되었는지 확인
        User verifiedUser = userRepository.findById(savedUser.get().getId()).orElseThrow();
        assertThat(verifiedUser).isActive();

        // 6. 이메일 인증 정보가 업데이트되었는지 확인
        EmailVerification updatedVerification = emailVerificationRepository
                .findById(verification.get().getId()).orElseThrow();
        assertThat(updatedVerification).isVerified();
    }

    @Test
    @DisplayName("이메일 재발송 통합 테스트")
    void emailResendIntegrationFlow() throws Exception {
        // 1. 사용자 생성 및 이메일 인증 정보 생성
        User user = TestFixtures.createUser("resend@example.com", "resenduser");
        userRepository.save(user);

        String token = tokenService.generateVerificationToken(
                user.getId(), user.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = TestFixtures.createEmailVerification(user, token);
        emailVerificationRepository.save(verification);

        // 2. 이메일 재발송 요청
        mockMvc.perform(post("/email-verifications/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "resend@example.com",
                        "type": "SIGNUP"
                    }
                    """))
                .andExpect(status().isOk());

        // 3. 기존 토큰으로 인증 가능한지 확인
        mockMvc.perform(get("/email-verifications/{token}", token))
                .andExpect(status().isOk());

        // 4. 사용자 상태 확인
        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(verifiedUser).isActive();
    }

    @Test
    @DisplayName("만료된 토큰으로 인증 시도 시 실패")
    void expiredTokenVerificationFails() throws Exception {
        // 1. 사용자 생성
        User user = TestFixtures.createUser("expired@example.com", "expireduser");
        userRepository.save(user);

        // 2. 만료된 이메일 인증 정보 생성
        EmailVerification expiredVerification = TestFixtures.createExpiredEmailVerification(user, "expired-token");
        emailVerificationRepository.save(expiredVerification);

        // 3. 만료된 토큰으로 인증 시도
        mockMvc.perform(get("/email-verifications/{token}", "expired-token"))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 달라질 수 있음

        // 4. 사용자 상태가 변경되지 않았는지 확인
        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchangedUser).isNotVerified();
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰으로 인증 시도 시 실패")
    void deletedUserTokenVerificationFails() throws Exception {
        // 1. 삭제된 사용자 생성
        User deletedUser = TestFixtures.createDeletedUser("deleted@example.com", "deleteduser");
        userRepository.save(deletedUser);

        // 2. 이메일 인증 정보 생성 (삭제 전에 생성된 토큰이라고 가정)
        String token = tokenService.generateVerificationToken(
                deletedUser.getId(), deletedUser.getEmail(), VerificationType.SIGNUP);

        EmailVerification verification = TestFixtures.createEmailVerification(deletedUser, token);
        emailVerificationRepository.save(verification);

        // 3. 삭제된 사용자의 토큰으로 인증 시도
        mockMvc.perform(get("/email-verifications/{token}", token))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 달라질 수 있음

        // 4. 사용자 상태가 변경되지 않았는지 확인
        User unchangedUser = userRepository.findById(deletedUser.getId()).orElseThrow();
        assertThat(unchangedUser).isDeleted();
    }
}