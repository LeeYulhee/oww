package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final MailProperties mailProperties;

    /**
     * 이메일 발송
     */
    public void sendEmail(String email, String emailToken) {
        try {
            String verificationUrl = buildVerificationUrl(emailToken);
            MimeMessage message = createVerificationEmailMessage(email, verificationUrl);
            sendEmailMessage(message, email);
        } catch (Exception e) {
            log.error("인증 이메일 발송 실패: {} - {}", email, e.getMessage());
            // TODO Exception 정의 필요
            throw new IllegalArgumentException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 이메일 인증 상태 완료로 변경(인증 날짜 설정)
     */
    public void updateEmailVerification(EmailVerification emailVerification) {
        emailVerification.updateEmailVerification();
    }

    /**
     * 이메일 인증 Entity 생성 및 저장
     */
    public void createEmailVerification(User user, String token) {
        EmailVerification verification = EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(appProperties.getVerificationTokenExpiry()))
                .user(user)
                .build();

        emailVerificationRepository.save(verification);
    }

    /**
     * 해당 토큰의 존재(유효성) 확인
     */
    public Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto parseTokenDto, String token, LocalDateTime now) {
        return emailVerificationRepository.findValidVerificationByParseToken(parseTokenDto, token, now);
    }

    /**
     * 이메일 인증 URL 생성
     */
    private String buildVerificationUrl(String emailToken) {
        return appProperties.getFrontendUrl() + "/email-verifications/" + emailToken;
    }

    /**
     * 이메일 생성
     */
    private MimeMessage createVerificationEmailMessage(String toEmail, String verificationUrl)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailProperties.getUsername());
        helper.setTo(toEmail);
        helper.setSubject("[오운완] 이메일 인증을 완료해주세요");
        helper.setText(buildVerificationEmailContent(verificationUrl), true);

        return message;
    }

    /**
     * 이메일 발송
     */
    private void sendEmailMessage(MimeMessage message, String toEmail) {
        mailSender.send(message);
        log.info("인증 이메일 발송 완료: {}", toEmail);
    }

    /**
     * 회원가입 이메일 내용 생성
     */
    private String buildVerificationEmailContent(String verificationUrl) {
        return """
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;">
                <h2 style="color: #333;">오운완 이메일 인증</h2>
                <p>안녕하세요! 오운완 회원가입을 환영합니다.</p>
                <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" 
                       style="background-color: #007bff; color: white; padding: 12px 24px; 
                              text-decoration: none; border-radius: 5px; display: inline-block;">
                        이메일 인증하기
                    </a>
                </div>
                <p><strong>주의:</strong> 이 링크는 24시간 후 만료됩니다.</p>
                <p>링크가 작동하지 않는다면 아래 URL을 복사하여 브라우저에 붙여넣어주세요:</p>
                <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;">
                    %s
                </p>
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    이 이메일은 오운완 시스템에서 자동으로 발송된 메일입니다.
                </p>
            </div>
            """.formatted(verificationUrl, verificationUrl);
    }
}
