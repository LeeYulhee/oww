package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.event.ResendVerificationEmailEvent;
import flobitt.oww.domain.user.event.SendVerificationEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 회원가입 및 회원가입 인증 이메일 발송
     */
    @Transactional
    public void signUp(CreateUserReq req) {
        User user = CreateUserReq.toEntity(req, passwordEncoder.encode(req.getPassword()));
        userService.createUser(user);

        String emailToken = tokenService.generateVerificationToken(user.getId(), user.getEmail(), VerificationType.SIGNUP);
        emailVerificationService.createEmailVerification(user, emailToken);

        applicationEventPublisher.publishEvent(new SendVerificationEmailEvent(user.getEmail(), emailToken));
    }

    public void resendEmail(ResendEmailReq req) {
        log.info("인증 이메일 재발송 요청: {}", req.getEmail());

        User user = userService.findByEmailAndIsDeletedFalse(req.getEmail());

        // 기존 토큰 조회 : user, type, verification = null
        EmailVerification verification = emailVerificationService.findByUserAndVerificationTypeAndVerificationAtIsNull(user, req.getType());

        // 이메일 재발송
        applicationEventPublisher.publishEvent(new ResendVerificationEmailEvent(user.getEmail(), verification.getVerificationToken()));

        log.info("인증 이메일 재발송 완료: userId={}", user.getUserLoginId());
    }

    /**
     * 이메일 인증
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("이메일 인증 시도: token={}", token);

        EmailVerification verification = getValidEmailVerification(token);
        User user = verification.getUser();

        userService.updateUserStatusActive(user);
        emailVerificationService.updateEmailVerification(verification);

        log.info("이메일 인증 완료: userId={}", user.getUserLoginId());
    }

    /**
     * 토큰을 검증하고 유효한 EmailVerification을 반환
     */
    private EmailVerification getValidEmailVerification(String token) {
        ParseTokenDto parseTokenDto = tokenService.validateToken(token);

        return emailVerificationService
                .findValidVerificationByParseToken(parseTokenDto, token, LocalDateTime.now());
    }
}
