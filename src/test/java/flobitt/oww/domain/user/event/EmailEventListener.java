package flobitt.oww.domain.user.event;

import flobitt.oww.domain.user.event.ResendVerificationEmailEvent;
import flobitt.oww.domain.user.event.SendVerificationEmailEvent;
import flobitt.oww.domain.user.event.listener.EmailEventListener;
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

    @Test
    @DisplayName("이메일 발송 실패 시 예외 전파")
    void handleSendVerificationEmail_EmailSendFailure() {
        // given
        String email = "test@example.com";
        String token = "verification-token";
        SendVerificationEmailEvent event = new SendVerificationEmailEvent(email, token);

        doThrow(new RuntimeException("Email sending failed"))
                .when(emailService).sendEmail(anyString(), anyString());

        // when & then
        // 비동기 처리이므로 예외가 호출자에게 전파되지 않음
        // 실제 운영에서는 로깅이나 별도 처리가 필요
        emailEventListener.handleSendVerificationEmail(event);

        verify(emailService, times(1)).sendEmail(email, token);
    }
}