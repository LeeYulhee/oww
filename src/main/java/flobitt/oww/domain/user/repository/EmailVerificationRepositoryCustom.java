package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.VerificationType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepositoryCustom {
    Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto dto, String token, LocalDateTime now);
    Optional<EmailVerification> findByUserAndVerificationTypeAndVerificationAtIsNull(User user, VerificationType type, LocalDateTime now);
}