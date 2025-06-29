package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.global.properties.AppProperties;
import flobitt.oww.global.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // 인증 토큰 생성
    public String generateVerificationToken(UUID userId, String email, VerificationType type) {
        SecretKey secretKey = getSecretKey(jwtProperties.getVerificationKey());

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("type", type)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(appProperties.getVerificationTokenExpiry() * 3600)))
                .signWith(secretKey)
                .compact();
    }

    private SecretKey getSecretKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
