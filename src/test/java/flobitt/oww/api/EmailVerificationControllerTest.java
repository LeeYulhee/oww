package flobitt.oww.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.entity.VerificationType;
import flobitt.oww.domain.user.service.AuthFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailVerificationController.class)
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthFacade authFacade;

    @Test
    @DisplayName("이메일 인증 요청 성공")
    void verifyEmail_Success() throws Exception {
        // given
        String token = "valid-verification-token";
        doNothing().when(authFacade).verifyEmail(anyString());

        // when & then
        mockMvc.perform(get("/email-verifications/{token}", token))
                .andExpect(status().isOk());

        verify(authFacade, times(1)).verifyEmail(token);
    }

    @Test
    @DisplayName("이메일 인증 요청 - 유효하지 않은 토큰으로 실패")
    void verifyEmail_InvalidToken_Fail() throws Exception {
        // given
        String invalidToken = "invalid-token";
        doThrow(new IllegalArgumentException("유효하지 않거나 만료된 인증 링크입니다."))
                .when(authFacade).verifyEmail(anyString());

        // when & then
        mockMvc.perform(get("/email-verifications/{token}", invalidToken))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 다를 수 있음

        verify(authFacade, times(1)).verifyEmail(invalidToken);
    }

    @Test
    @DisplayName("인증 이메일 재발송 요청 성공")
    void resendEmail_Success() throws Exception {
        // given
        ResendEmailReq request = ResendEmailReq.builder()
                .email("test@example.com")
                .type(VerificationType.SIGNUP)
                .build();

        doNothing().when(authFacade).resendEmail(any(ResendEmailReq.class));

        // when & then
        mockMvc.perform(post("/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authFacade, times(1)).resendEmail(any(ResendEmailReq.class));
    }

    @Test
    @DisplayName("인증 이메일 재발송 요청 - 존재하지 않는 사용자로 실패")
    void resendEmail_UserNotFound_Fail() throws Exception {
        // given
        ResendEmailReq request = ResendEmailReq.builder()
                .email("nonexistent@example.com")
                .type(VerificationType.SIGNUP)
                .build();

        doThrow(new IllegalArgumentException("유효한 회원 정보가 없습니다."))
                .when(authFacade).resendEmail(any(ResendEmailReq.class));

        // when & then
        mockMvc.perform(post("/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // 예외 처리에 따라 다를 수 있음

        verify(authFacade, times(1)).resendEmail(any(ResendEmailReq.class));
    }
}