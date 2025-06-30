package flobitt.oww.domain.user.event.listener;

import flobitt.oww.domain.user.event.CreateUserEvent;
import flobitt.oww.domain.user.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
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
    public void handleCreateUser(CreateUserEvent event) {
        emailService.sendEmail(event.getEmail(), event.getToken());
    }
}
