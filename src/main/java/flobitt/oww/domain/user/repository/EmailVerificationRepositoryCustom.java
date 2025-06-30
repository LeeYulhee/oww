package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.dto.internal.ParseTokenDto;
import flobitt.oww.domain.user.entity.EmailVerification;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepositoryCustom {
    Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto dto, String token, LocalDateTime now);
}
