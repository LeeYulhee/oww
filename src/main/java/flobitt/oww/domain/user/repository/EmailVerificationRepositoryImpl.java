package flobitt.oww.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.QEmailVerification;
import flobitt.oww.domain.user.entity.VerificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class EmailVerificationRepositoryImpl implements EmailVerificationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 해당 토큰의 존재(유효성) 확인
     */
    @Override
    public Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto dto, String token, LocalDateTime now) {
        QEmailVerification emailVerification = QEmailVerification.emailVerification;

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(emailVerification)
                .where(emailVerification.user.id.eq(UUID.fromString(dto.getUserId())),
                        emailVerification.email.eq(dto.getEmail()),
                        emailVerification.verificationType.eq(VerificationType.valueOf(dto.getTokenType())),
                        emailVerification.verificationToken.eq(token),
                        emailVerification.expiresAt.gt(now),
                        emailVerification.verifiedAt.isNull())
                .fetchOne());
    }
}
