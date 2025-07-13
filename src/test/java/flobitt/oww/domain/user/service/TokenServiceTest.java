package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock

    private AppProperties appProperties;

    @Spy
    private SecureRandom secureRandom = new SecureRandom();

    @InjectMocks
    private TokenService tokenService;

    private final String TEST_SECRET_KEY = "test-secret-key-for-verification-tokens-must-be-long-enough";

    private void setupJwtMock() {
        given(jwtProperties.getVerificationKey()).willReturn(TEST_SECRET_KEY);
    }

    private void setupAppMock() {
        given(appProperties.getVerificationTokenExpiry()).willReturn(24);
    }

    private void setupAllMocks() {
        setupJwtMock();
        setupAppMock();
    }

    @Test
    @DisplayName("인증 토큰 생성 성공")

    void generateVerificationToken_Success() {
        // given
        setupAllMocks();
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        VerificationType type = VerificationType.SIGNUP;

        // when
        String token = tokenService.generateVerificationToken(email, type);

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
        setupAllMocks();
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        VerificationType type = VerificationType.SIGNUP;

        String token = tokenService.generateVerificationToken(email, type);

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
        setupJwtMock();
        String invalidToken = "invalid.token.here";

        // when & then
        assertThatThrownBy(() -> tokenService.validateToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
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
    @DisplayName("빈 이메일로 토큰 검증 시 예외 발생")
    void validateToken_EmptyEmail_ThrowsException() {
        // given
        setupJwtMock();
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        String tokenWithEmptyEmail = Jwts.builder()
                .claim("email", "")
                .claim("type", "SIGNUP")
                .claim("nonce", "test-nonce")
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        // when & then
        assertThatThrownBy(() -> tokenService.validateToken(tokenWithEmptyEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("빈 토큰 타입으로 토큰 검증 시 예외 발생")
    void validateToken_EmptyTokenType_ThrowsException() {
        // given
        setupJwtMock();
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        String tokenWithEmptyType = Jwts.builder()
                .claim("email", "test@example.com")
                .claim("type", "")
                .claim("nonce", "test-nonce")
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        // when & then
        assertThatThrownBy(() -> tokenService.validateToken(tokenWithEmptyType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("Nonce 생성 테스트 - Mock 사용")
    void generateSecureNonce_WithMock_Success() {
        // given
        byte[] mockBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

        // doAnswer 사용 (void 메서드용)
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            System.arraycopy(mockBytes, 0, bytes, 0, 8);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        // when
        String nonce = tokenService.generateSecureNonce();

        // then
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(16); // 8바이트 -> 16진수 16문자
        assertThat(nonce).matches("[0-9a-f]{16}"); // 16진수 패턴 확인

        // 예상 결과: "0102030405060708" (mockBytes를 16진수로 변환한 값)
        assertThat(nonce).isEqualTo("0102030405060708");
    }

    @Test
    @DisplayName("Nonce 생성 테스트 - 실제 동작")
    void generateSecureNonce_RealBehavior_Success() {
        // given & when (실제 SecureRandom 사용)
        String nonce1 = tokenService.generateSecureNonce();
        String nonce2 = tokenService.generateSecureNonce();

        // then
        assertThat(nonce1).isNotNull();
        assertThat(nonce1).hasSize(16);
        assertThat(nonce1).matches("[0-9a-f]{16}");

        assertThat(nonce2).isNotNull();
        assertThat(nonce2).hasSize(16);
        assertThat(nonce2).matches("[0-9a-f]{16}");

        // 랜덤성 확인
        assertThat(nonce1).isNotEqualTo(nonce2);
    }
}