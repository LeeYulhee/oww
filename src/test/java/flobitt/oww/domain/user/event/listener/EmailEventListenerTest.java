package flobitt.oww.domain.user.event.listener;

import flobitt.oww.domain.user.event.ResendVerificationEmailEvent;
import flobitt.oww.domain.user.event.SendVerificationEmailEvent;
import flobitt.oww.domain.user.service.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventListenerTest {

    @Mock
    private EmailVerificationService emailService;

    @InjectMocks
    private EmailEventListener emailEventListener;

    @Test
    @DisplayName("회원가입 인증 이메일 발송 이벤트 처리")
    void handleSendVerificationEmail_Success() {
        // given
        String email = "test@example.com";
        String token = "verification-token";
        SendVerificationEmailEvent event = new SendVerificationEmailEvent(email, token);

        doNothing().when(emailService).sendEmail(anyString(), anyString());

        // when
        emailEventListener.handleSendVerificationEmail(event);

        // then
        verify(emailService, times(1)).sendEmail(email, token);
    }

    @Test
    @DisplayName("인증 이메일 재발송 이벤트 처리")
    void handleResendVerificationEmail_Success() {
        // given
        String email = "test@example.com";
        String token = "verification-token";
        ResendVerificationEmailEvent event = new ResendVerificationEmailEvent(email, token);

        doNothing().when(emailService).sendEmail(anyString(), anyString());

        // when
        emailEventListener.handleResendVerificationEmail(event);

        // then
        verify(emailService, times(1)).sendEmail(email, token);
    }
}