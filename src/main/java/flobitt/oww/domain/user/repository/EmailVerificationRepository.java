package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID>, EmailVerificationRepositoryCustom {
}
