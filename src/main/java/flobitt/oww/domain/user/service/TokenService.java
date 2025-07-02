package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;

    /**
     * 인증 토큰 생성
     */
    public String generateVerificationToken(UUID userId, String email, VerificationType type) {
        SecretKey secretKey = getSecretKey(jwtProperties.getVerificationKey());

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("type", type.toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(appProperties.getVerificationTokenExpiry() * 3600)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰 검증
     */
    public ParseTokenDto validateToken(String token) {
        Claims tokenClaims = parseToken(token);

        ParseTokenDto parseTokenDto = ParseTokenDto.builder()
                .userId(tokenClaims.getSubject())
                .email(tokenClaims.get("email", String.class))
                .tokenType(tokenClaims.get("type", String.class))
                .build();

        // TODO Exception 설정
        if (parseTokenDto.getTokenType().isBlank()
            || parseTokenDto.getUserId().isBlank()
            || parseTokenDto.getEmail().isBlank()) throw new IllegalArgumentException("유효하지 않은 토큰입니다.");

        return parseTokenDto;
    }

    /**
     * JWT 토큰 파싱
     */
    private Claims parseToken(String token) {
        SecretKey secretKey = getSecretKey(jwtProperties.getVerificationKey());

        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * JWT 토큰 파싱을 위한 secretKey 생성
     */
    private SecretKey getSecretKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
