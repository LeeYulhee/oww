package flobitt.oww.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 유효성 검사 통합 테스트
 */
@AutoConfigureMockMvc
class ValidationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "ab", "abc"}) // 4자 미만
    @DisplayName("로그인 ID 길이 유효성 검사")
    void loginIdLengthValidation(String invalidLoginId) throws Exception {
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId(invalidLoginId)
                .email("test@example.com")
                .password("Test123!@#")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",
        "@example.com",
        "test@",
        "test..test@example.com",
        "test@.com"
    })
    @DisplayName("이메일 형식 유효성 검사")
    void emailFormatValidation(String invalidEmail) throws Exception {
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email(invalidEmail)
                .password("Test123!@#")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "weak",           // 너무 짧음
        "12345678",       // 숫자만
        "abcdefgh",       // 영문만
        "Test1234",       // 특수문자 없음
        "Test!@#$",       // 숫자 없음
        "1234!@#$"        // 영문 없음
    })
    @DisplayName("비밀번호 규칙 유효성 검사")
    void passwordRuleValidation(String invalidPassword) throws Exception {
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("testuser")
                .email("test@example.com")
                .password(invalidPassword)
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모든 필드가 유효한 경우 성공")
    void validRequestSuccess() throws Exception {
        CreateUserReq request = CreateUserReq.builder()
                .userLoginId("validuser")
                .email("valid@example.com")
                .password("Valid123!@#")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}