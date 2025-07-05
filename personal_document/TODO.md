### 로그 파일
- 배치는 별도 로그 파일 생성
---

### 리팩토링 예시 : UserService
```java
public SignupResponse signup(SignupRequest request) {
    validateDuplicateUser(request);
    
    User user = createAndSaveUser(request);
    sendVerificationEmail(user);
    
    return SignupResponse.from(user); // 정적 팩토리 메서드
}

private User createAndSaveUser(SignupRequest request) {
    User user = User.builder()
            .userLoginId(request.getUserLoginId())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .userStatus(UserStatus.NOT_VERIFIED)
            .build();
    
    return userRepository.save(user); // 저장된 user 반환
}
```
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class SignupResponse {
    private String userId;
    private String email;
    private String message;
}
```
---
