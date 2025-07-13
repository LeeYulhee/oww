package flobitt.oww.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;

import static org.mockito.Mockito.*;

/**
 * 테스트 전용 Bean 설정
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public JavaMailSender testMailSender() {
        JavaMailSender mockSender = mock(JavaMailSender.class);

        // 실제 MimeMessage 객체 반환
        when(mockSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        // 이메일 발송은 무시
        doNothing().when(mockSender).send(any(MimeMessage.class));

        return mockSender;
    }

    @Bean
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecureRandom testSecureRandom() {
        return new SecureRandom();
    }
}