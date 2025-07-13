# 컨트롤러 테스트

## 컨트롤러 테스트의 핵심 역할

**컨트롤러의 책임만 테스트**하는 것이 컨트롤러 테스트입니다.

### 컨트롤러가 담당하는 것들:

1. **HTTP 요청 받기** (URL, 메서드, 파라미터)
2. **요청 데이터 유효성 검사** (@Valid, @NotNull 등)
3. **적절한 서비스 메서드 호출** (비즈니스 로직은 서비스에 위임)
4. **HTTP 응답 반환** (상태코드, 응답 본문)

### 그래서 컨트롤러 테스트에서는:

```java
// ✅ 이런 것들을 확인
.andExpect(status().isBadRequest())           // 올바른 상태코드 반환?
verify(authFacade, times(1)).signUp(...)     // 서비스를 정확히 1번 호출?
verify(authFacade, never()).signUp(...)      // 에러 시 서비스 호출 안 함?
```

```java
// ❌ 이런 건 확인하지 않음 (서비스 테스트의 영역)
// - 실제 DB에 데이터가 저장되었는지
// - 비즈니스 로직이 올바르게 동작하는지
// - 이메일이 실제로 발송되는지
```

## 테스트 계층 분리

```java
@WebMvcTest(UserController.class)  // 컨트롤러만 테스트
class UserControllerTest {
    @MockitoBean
    private AuthFacade authFacade;  // 서비스는 Mock으로 대체
    
    // 컨트롤러의 라우팅, 유효성검사, 서비스호출만 확인
}

@ExtendWith(MockitoExtension.class)  // 서비스만 테스트  
class AuthFacadeTest {
    @Mock
    private UserService userService;
    
    // 실제 비즈니스 로직 테스트
}

@DataJpaTest  // Repository만 테스트
class UserRepositoryTest {
    // 실제 DB 연동 테스트
}
```

## 정리

컨트롤러 테스트 = **"내가 올바른 서비스를 올바른 타이밍에 호출했나?"** 를 확인하는 테스트

- 성공 케이스: `verify(service, times(1)).method()`
- 실패 케이스: `verify(service, never()).method()`

실제 비즈니스 로직이 맞는지는 서비스 테스트에서 따로 확인하는 거죠!

## 컨트롤러 테스트가 포함하는 계층

```

HTTP 요청 → Controller → DTO 유효성 검사 → Service 호출 (Mock)
    ↑                      ↑              ↑
여기까지가              여기서 걸러냄      실제 로직은 안 들어감
컨트롤러 테스트 범위

```

### 1. **HTTP 계층** ✅

```java
post("/users")                           // URL 매핑
.contentType(MediaType.APPLICATION_JSON) // Content-Type 처리
```

### 2. **DTO 변환 및 유효성 검사** ✅

```java
// CreateUserReq의 @Valid 어노테이션들이 동작
@NotBlank(message = "로그인 ID는 필수입니다")
private String userLoginId;

@Email(message = "올바른 이메일 형식이 아닙니다")  
private String email;

@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")
private String password;
```

### 3. **컨트롤러 메서드** ✅

```java
@PostMapping("/users")
public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserReq request) {
    authFacade.signUp(request);  // 여기까지만 확인
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

### 4. **Service/Facade 호출** ❌ (Mock으로 대체)

```java
@MockitoBean
private AuthFacade authFacade;  // 실제 로직은 실행되지 않음

// 단지 호출 여부만 확인
verify(authFacade, times(1)).signUp(any(CreateUserReq.class));
```

## 실제 테스트에서 유효성 검사가 동작하는 예시

```java
@Test
@DisplayName("회원가입 요청 - 잘못된 이메일 형식으로 실패")
void createUser_InvalidEmail_Fail() throws Exception {
    CreateUserReq request = CreateUserReq.builder()
            .email("invalid-email")  // @Email 어노테이션에 걸림
            .build();

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());  // 400 에러 발생
            
    // 유효성 검사에서 걸렸으므로 서비스 호출 안 됨
    verify(authFacade, never()).signUp(any(CreateUserReq.class));
}
```

## 정리

컨트롤러 테스트는 **"웹 요청이 들어와서 서비스를 호출하기 직전까지"** 의 모든 과정을 테스트합니다:

- ✅ URL 라우팅
- ✅ HTTP 메서드 매핑
- ✅ 요청 본문 → DTO 변환
- ✅ **DTO 유효성 검사** (가장 중요!)
- ✅ 서비스 호출 여부
- ❌ 실제 비즈니스 로직 (서비스 테스트에서)

그래서 잘못된 데이터가 서비스 계층으로 넘어가지 않도록 **컨트롤러에서 1차 방어선** 역할을 제대로 하는지 확인하는 테스트입니다!

## @WebMvcTest의 역할

### 1. **웹 계층만 로딩** 🎯

```java
@WebMvcTest(UserController.class)
```

- 전체 Spring Context를 띄우지 않고 **웹 관련 빈들만** 로딩
- UserController와 웹 계층에 필요한 최소한의 컴포넌트만 활성화

### 2. **포함되는 것들** ✅

```java
// 자동으로 로딩되는 빈들
@Controller, @RestController     // 컨트롤러들
@ControllerAdvice               // 예외 처리
@JsonComponent                  // JSON 변환
MockMvc                        // 테스트용 MockMvc 자동 설정
ObjectMapper                   // JSON 매핑
Validator                      // 유효성 검사 (@Valid)
```

### 3. **제외되는 것들** ❌

```java
// 로딩되지 않는 빈들
@Service, @Repository          // 서비스, 리포지토리 계층
@Component                     // 일반 컴포넌트
Database 관련 설정             // JPA, DataSource 등
```

## 다른 테스트 어노테이션과 비교

```java
// 1. 전체 컨텍스트 로딩 (무겁고 느림)
@SpringBootTest
class IntegrationTest {
    // 모든 빈이 로딩됨 (DB, 서비스, 컨트롤러 등)
}

// 2. 웹 계층만 로딩 (가볍고 빠름) ⭐
@WebMvcTest(UserController.class) 
class UserControllerTest {
    // 웹 관련 빈만 로딩
}

// 3. JPA 계층만 로딩
@DataJpaTest
class UserRepositoryTest {
    // Repository, Entity, 임베디드 DB만 로딩
}
```

## 실제 동작 예시

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;        // ✅ 자동 주입됨
    
    @Autowired  
    private ObjectMapper objectMapper; // ✅ 자동 주입됨
    
    @MockitoBean
    private AuthFacade authFacade;  // ❌ 실제 빈이 없으므로 Mock으로 대체 필요
    
    // UserController만 실제 빈으로 로딩됨
    // 나머지 서비스들은 Mock으로 대체해야 함
}
```

## 장점

### 1. **빠른 실행 속도** ⚡

- 필요한 빈만 로딩하므로 컨텍스트 시작 시간이 짧음
- 단위 테스트에 가까운 속도

### 2. **명확한 테스트 범위** 🎯

- 웹 계층만 테스트한다는 의도가 명확
- 다른 계층의 영향을 받지 않음

### 3. **격리된 테스트** 🔒

- 서비스나 DB 문제가 컨트롤러 테스트에 영향을 주지 않음

## 정리

`@WebMvcTest`는 **"컨트롤러만 집중해서 테스트하겠다"** 는 의미입니다!

- 빠르고 가벼운 테스트
- 웹 계층(컨트롤러, 유효성 검사, HTTP 매핑)만 검증
- 나머지 계층은 Mock으로 대체하여 격리된 테스트 환경 제공