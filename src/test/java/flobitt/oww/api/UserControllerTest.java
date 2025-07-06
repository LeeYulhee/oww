package flobitt.oww.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.service.AuthFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthFacade authFacade;

    @Test
    @DisplayName("회원가입 요청 성공")
    void createUser_Success() throws Exception {
        // given
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("Test123!@#")
                .build();

        doNothing().when(authFacade).signUp(any(CreateUserReq.class));

        // when & then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authFacade, times(1)).signUp(any(CreateUserReq.class));
    }

    @Test
    @DisplayName("회원가입 요청 - 잘못된 이메일 형식으로 실패")
    void createUser_InvalidEmail_Fail() throws Exception {
        // given
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("invalid-email")  // 잘못된 이메일 형식
                .password("Test123!@#")
                .build();

        // when & then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authFacade, never()).signUp(any(CreateUserReq.class));
    }

    @Test
    @DisplayName("회원가입 요청 - 비밀번호 유효성 검사 실패")
    void createUser_InvalidPassword_Fail() throws Exception {
        // given
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password("weak")  // 비밀번호 규칙 미충족
                .build();

        // when & then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isBadRequest());

        verify(authFacade, never()).signUp(any(CreateUserReq.class));
    }

    @Test
    @DisplayName("회원가입 요청 - 필수 필드 누락으로 실패")
    void createUser_MissingRequiredFields_Fail() throws Exception {
        // given
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("")  // 빈 값
                .email("test@example.com")
                .password("Test123!@#")
                .build();

        // when & then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authFacade, never()).signUp(any(CreateUserReq.class));
    }

    @Test
    @DisplayName("회원가입 요청 - JSON 형식 오류로 실패")
    void createUser_InvalidJson_Fail() throws Exception {
        // given
        String invalidJson = "{ invalid json }";

        // when & then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(authFacade, never()).signUp(any(CreateUserReq.class));
    }
}