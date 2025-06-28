package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    // Optional<EmailVerification> findValidVerification(String token, VerificationType type, LocalDateTime now);
}
