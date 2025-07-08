### 기본 역할

**JUnit 5의 확장 메커니즘<br>**
@ExtendWith는 JUnit 5에서 테스트에 추가 기능을 제공하는 확장(Extension)을 등록하는 어노테이션입니다.
```java
@ExtendWith(MockitoExtension.class)  // Mockito 확장 활성화
class MyTest {
    // 이제 Mockito 기능을 사용할 수 있음
}
```

### MockitoExtension이 하는 일들
1. Mock 객체 자동 생성
```java
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    
    @Mock
    private JwtProperties jwtProperties;  // ✅ 자동으로 Mock 객체 생성
    
    @Mock  
    private AppProperties appProperties;  // ✅ 자동으로 Mock 객체 생성
    
    @InjectMocks
    private TokenService tokenService;    // ✅ Mock들을 자동으로 주입받는 실제 객체
}
```

2. @Mock vs 수동 생성 비교<br>
❌ MockitoExtension 없이 (수동)
    ```java
    class TokenServiceTest {
    
        private JwtProperties jwtProperties;
        private AppProperties appProperties;
        private TokenService tokenService;
    
        @BeforeEach
        void setUp() {
            // 수동으로 Mock 객체 생성
            jwtProperties = Mockito.mock(JwtProperties.class);
            appProperties = Mockito.mock(AppProperties.class);
    
            // 수동으로 의존성 주입
            tokenService = new TokenService(jwtProperties, appProperties);
        }
    }
    ```
   ✅ MockitoExtension 사용 (자동)
    ```java
    @ExtendWith(MockitoExtension.class)
    class TokenServiceTest {
        
        @Mock
        private JwtProperties jwtProperties;  // 자동 생성
        
        @Mock
        private AppProperties appProperties;  // 자동 생성
        
        @InjectMocks  
        private TokenService tokenService;    // 자동 주입
        
        // setUp 메서드 불필요!
    }
    ```
   
### 🏗️ MockitoExtension의 생명주기
**테스트 실행 과정**
```java
1. 테스트 클래스 로드
   ↓
2. MockitoExtension 활성화
   ↓  
3. @Mock 어노테이션 발견 → Mock 객체 생성
   ↓
4. @InjectMocks 어노테이션 발견 → 의존성 주입
   ↓
5. @BeforeEach 메서드 실행
   ↓
6. 실제 테스트 메서드 실행
   ↓
7. @AfterEach 메서드 실행
   ↓
8. Mock 객체들 정리
```

### MockitoExtension이 지원하는 어노테이션들
- @Mock
    ```java
    @Mock
    private UserRepository userRepository;  // Mock 객체 생성
    ```

- @Spy
    ```java
    @Spy
    private List<String> spyList = new ArrayList<>();  // 실제 객체를 감싸는 Spy 생성
    // Spy : 대부분은 실제 로직을 사용하고, 특정 메서드만 테스트용으로 바꾸고 싶을 때
    ```
    ```java
    @ExtendWith(MockitoExtension.class)
    class SpyExample {
        
        @Spy
        private List<String> spyList = new ArrayList<>();  // 실제 ArrayList, 하지만 감시됨
        
        @Test
        void spy_Example() {
            // 실제 동작 - 진짜 ArrayList처럼 동작
            spyList.add("진짜");
            spyList.add("데이터");
            assertThat(spyList).hasSize(2);  // 실제로 2개 들어감
            
            // 특정 메서드만 가짜로 만들기
            when(spyList.size()).thenReturn(100);  // size()만 가짜 결과 반환
            
            assertThat(spyList.size()).isEqualTo(100);    // 가짜: 100 반환
            assertThat(spyList.get(0)).isEqualTo("진짜");  // 실제: 진짜 데이터 반환
        }
    }
    ```
    ```java
    // 예: 대부분은 실제 로직을 사용하고, 특정 메서드만 테스트용으로 바꾸고 싶을 때
    @Spy
    private EmailService emailService = new EmailService();
    
    @Test
    void test() {
        // 실제 이메일 발송은 막고, 다른 로직은 실제로 실행
        doNothing().when(emailService).sendEmail(anyString());
        
        emailService.processEmailQueue();  // 실제 로직 실행, 단 이메일만 안 보냄
    }
    ```

- @InjectMocks
    ```java
    @InjectMocks
    private UserService userService;  // Mock들을 주입받는 실제 테스트 대상
    ```

- @Captor
    ```java
    @Captor
    private ArgumentCaptor<User> userCaptor;  // 인수 캡처용
    // Captor : 메서드에 전달된 인수(파라미터)를 몰래 훔쳐와서 확인하는 것
    ```
    ```java
    @ExtendWith(MockitoExtension.class)
    class CaptorExample {
        
        @Mock
        private UserRepository userRepository;
        
        @Captor
        private ArgumentCaptor<User> userCaptor;  // User 객체를 포획하는 도구
        
        @InjectMocks
        private UserService userService;
        
        @Test
        void captor_Example() {
            // given
            String email = "test@example.com";
            
            // when
            userService.createUser(email, "password");
            
            // then - userRepository.save()에 전달된 User 객체를 포획!
            verify(userRepository).save(userCaptor.capture());
            
            // 포획한 User 객체 확인
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(capturedUser.getPassword()).isNotNull();
        }
    }
    ```
    ```java
    // UserService.createUser() 내부 코드
    public void createUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(encode(password))  // 비밀번호 암호화
                .status(UserStatus.ACTIVE)   // 기본 상태
                .createdAt(LocalDateTime.now())
                .build();
        
        userRepository.save(user);  // 🎯 이 User 객체를 확인하고 싶음!
    }
    
    // 테스트에서 확인하고 싶은 것:
    // "정말로 비밀번호가 암호화되었나?"
    // "기본 상태가 ACTIVE로 설정되었나?"
    // "생성 시간이 제대로 설정되었나?"
    ```

### 실제 동작 예시
TokenService 클래스 구조
```java
@Service
public class TokenService {
    
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom;
    
    public TokenService(JwtProperties jwtProperties, 
                       AppProperties appProperties,
                       SecureRandom secureRandom) {
        this.jwtProperties = jwtProperties;
        this.appProperties = appProperties;
        this.secureRandom = secureRandom;
    }
    
    // 메서드들...
}
```

MockitoExtension의 자동 주입 과정
```java
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    
    @Mock
    private JwtProperties jwtProperties;    // 1. Mock 생성
    
    @Mock  
    private AppProperties appProperties;    // 2. Mock 생성
    
    @Mock
    private SecureRandom secureRandom;      // 3. Mock 생성
    
    @InjectMocks
    private TokenService tokenService;      // 4. 위 Mock들을 생성자에 주입하여 생성
    
    @Test
    void test() {
        // 이미 tokenService는 Mock들이 주입된 상태!
        assertThat(tokenService).isNotNull();
    }
}
```

### 다른 확장들과의 비교
Spring Boot Test와 함께 사용
```java
@ExtendWith({MockitoExtension.class, SpringExtension.class})
class MixedTest {
    
    @Mock
    private ExternalService externalService;  // Mockito Mock
    
    @Autowired  
    private UserService userService;          // Spring Bean
    
    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        ExternalService externalService(@Mock ExternalService mock) {
            return mock;  // Mock을 Spring Context에 등록
        }
    }
}
```

여러 확장 동시 사용
```java
@ExtendWith({
    MockitoExtension.class,    // Mockito 기능
    SpringExtension.class,     // Spring 기능  
    TempDirectoryExtension.class  // 임시 디렉토리 기능
})
class ComplexTest {
    // 모든 확장의 기능을 사용 가능
}
```

### MockitoExtension의 장점
- 간결성
    ```java
    // 3줄로 Mock 설정 완료
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;  
    @InjectMocks private UserService userService;
    ```
  
- 자동 정리
    ```java
    // 테스트 완료 후 자동으로 Mock 상태 초기화
    // 다음 테스트에 영향 없음
    ```
  
- 타입 안전성
    ```java
    @Mock
    private UserRepository userRepository;  // 컴파일 타임에 타입 체크
    ```
  
- IDE 지원
    ```java
    // IDE에서 Mock 객체임을 인식하여 자동완성 지원
    userRepository.findById(1L);  // Mock 메서드 자동완성
    ```
  
### 언제 사용해야 할까?
✅ 사용하면 좋은 경우<br>
- 단위 테스트: 외부 의존성을 Mock으로 대체
- Service 레이어 테스트: Repository, 외부 API 등을 Mock
- 복잡한 의존성: 여러 의존성을 가진 클래스 테스트

🤔 굳이 필요없는 경우<br>
- 통합 테스트: 실제 Bean들을 사용하는 경우
- Entity 테스트: 의존성이 없는 단순 객체 테스트
- Repository 테스트: @DataJpaTest로 실제 DB 사용