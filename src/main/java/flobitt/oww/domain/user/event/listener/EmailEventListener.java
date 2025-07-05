package flobitt.oww.domain.user.event.listener;

import flobitt.oww.domain.user.event.ResendVerificationEmailEvent;
import flobitt.oww.domain.user.event.SendVerificationEmailEvent;
import flobitt.oww.domain.user.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class EmailEventListener {
    private final EmailVerificationService emailService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Async
    public void handleSendVerificationEmail(SendVerificationEmailEvent event) {
        emailService.sendEmail(event.getEmail(), event.getToken());
    }

    @EventListener
    @Async
    public void handleResendVerificationEmail(ResendVerificationEmailEvent event) {
        emailService.sendEmail(event.getEmail(), event.getToken());
    }
}
