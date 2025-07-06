package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AppProperties appProperties;

    @Mock
    private SecureRandom secureRandom;

    @InjectMocks
    private TokenService tokenService;

    private final String TEST_SECRET_KEY = "test-secret-key-for-verification-tokens-must-be-long-enough";

    @BeforeEach
    void setUp() {
        given(jwtProperties.getVerificationKey()).willReturn(TEST_SECRET_KEY);
        given(appProperties.getVerificationTokenExpiry()).willReturn(24);
    }

    @Test
    @DisplayName("인증 토큰 생성 성공")
    void generateVerificationToken_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        VerificationType type = VerificationType.SIGNUP;

        // when
        String token = tokenService.generateVerificationToken(userId, email, type);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // 토큰 파싱해서 내용 확인
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("type", String.class)).isEqualTo(type.toString());
        assertThat(claims.get("nonce", String.class)).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("토큰 검증 성공")
    void validateToken_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        VerificationType type = VerificationType.SIGNUP;

        String token = tokenService.generateVerificationToken(userId, email, type);

        // when
        ParseTokenDto result = tokenService.validateToken(token);

        // then
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getTokenType()).isEqualTo(type.toString());
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 시 예외 발생")
    void validateToken_InvalidToken_ThrowsException() {
        // given
        String invalidToken = "invalid.token.here";

        // when & then
        assertThatThrownBy(() -> tokenService.validateToken(invalidToken))
                .isInstanceOf(Exception.class);  // JWT 파싱 예외
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 예외 발생")
    void validateToken_ExpiredToken_ThrowsException() {
        // given
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .claim("email", "test@example.com")
                .claim("type", "SIGNUP")
                .claim("nonce", "test-nonce")
                .setExpiration(Date.from(Instant.now().minusSeconds(3600))) // 1시간 전 만료
                .signWith(secretKey)
                .compact();

        // when & then
        assertThatThrownBy(() -> tokenService.validateToken(expiredToken))
                .isInstanceOf(Exception.class);  // JWT 만료 예외
    }

    @Test
    @DisplayName("Nonce 생성 테스트")
    void generateSecureNonce_Success() {
        // given
        byte[] mockBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        given(secureRandom.nextBytes(any(byte[].class)))
                .willAnswer(invocation -> {
                    byte[] bytes = invocation.getArgument(0);
                    System.arraycopy(mockBytes, 0, bytes, 0, 8);
                    return null;
                });

        // when
        String nonce = tokenService.generateSecureNonce();

        // then
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(16); // 8바이트 -> 16진수 16문자
        assertThat(nonce).matches("[0-9a-f]{16}"); // 16진수 패턴 확인
    }
}