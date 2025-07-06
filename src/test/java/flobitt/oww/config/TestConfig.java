package flobitt.oww.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;

import static org.mockito.Mockito.mock;

/**
 * 테스트 전용 Bean 설정
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public JavaMailSender testMailSender() {
        return mock(JavaMailSender.class);
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