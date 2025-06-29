package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public void signUp(CreateUserReq req) {
        User user = CreateUserReq.toEntity(req, passwordEncoder.encode(req.getPassword()));
        userService.create(user);

        String emailToken = tokenService.generateVerificationToken(user.getId(), user.getEmail(), VerificationType.SIGNUP);
        emailVerificationService.sendEmail(user, emailToken);
    }

    // 이메일 인증
    public void verifyEmail(String token) {
        log.info("이메일 인증 시도: token={}", token);

        // 토큰을 풀어서 유효성 검사 필요 : User 이메일, VerificationType 확인 가능

        // 1. 토큰 유효성 검사
        EmailVerification verification = emailVerificationService
                .findValidVerification(token, VerificationType.SIGNUP, LocalDateTime.now())
                // TODO Exception 설정
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 인증 링크입니다."));

        User user = verification.getUser();

        // 2. 사용자 상태 업데이트
        userService.updateUserStatusActive(user);

        // 3. 인증 완료 처리
        emailVerificationService.updateEmailVerification(verification);

        log.info("이메일 인증 완료: userId={}", user.getUserLoginId());
    }

    public void resendVerificationEmail(ResendEmailReq req) {
        log.info("인증 이메일 재발송 요청: {}", req.getEmail());

        // 1. 사용자 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("등록되지 않은 이메일입니다."));

        if (user.getUserStatus() == UserStatus.ACTIVE) {
            throw new IllegalStateException("이미 인증된 계정입니다.");
        }

        // 2. 기존 인증 토큰 무효화
        emailVerificationRepository.invalidatePreviousVerifications(
                user.getUserId(), VerificationType.SIGNUP, LocalDateTime.now());

        // 3. 새 인증 토큰 생성 및 발송
        String verificationToken = generateVerificationToken();
        createEmailVerification(user, verificationToken);
        sendEmail(user.getEmail(), verificationToken);

        log.info("인증 이메일 재발송 완료: userId={}", user.getUserId());
    }
}
