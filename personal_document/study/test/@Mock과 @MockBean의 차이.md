@MockBean과 @Mock은 둘 다 Mock 객체를 생성하지만, Spring 컨텍스트와의 연동 방식이 다릅니다.

### @Mock (Mockito)
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository; // 단순 Mock 객체
    
    @InjectMocks
    private UserService userService; // Mock을 수동으로 주입
    
    @Test
    void testFindUser() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        
        // when & then
        assertThat(userService.findUser(1L)).isNotNull();
    }
}
```
- 특징:
  - Mockito에서 제공
  - Spring 컨텍스트 없이 동작
  - 빠른 단위 테스트에 적합
  - @InjectMocks로 수동 주입 필요

### @MockBean (Spring Boot Test)
```java
@SpringBootTest
class UserServiceIntegrationTest {
    
    @MockBean
    private UserRepository userRepository; // Spring 빈을 Mock으로 교체
    
    @Autowired
    private UserService userService; // Spring이 자동 주입 (Mock 포함)
    
    @Test
    void testFindUser() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        
        // when & then
        assertThat(userService.findUser(1L)).isNotNull();
    }
}
```
- 특징:
  - Spring Boot Test에서 제공
  - Spring 컨텍스트와 함께 동작
  - 실제 빈을 Mock으로 교체
  - Spring의 자동 주입 활용

### 주요 차이점
| 구분          | @Mock   | @MockBean        |
|-------------|---------|------------------|
| 제공자         | Mockito | Spring Boot Test |
| Spring 컨텍스트 | 사용 안함   | 사용함              |
| 속도          | 빠름      | 상대적으로 느림         |
| 사용 목적       | 단위 테스트  | 통합 테스트           |
| 빈 교체        | 안함      | 실제 빈을 Mock으로 교체  |

### 실제 사용 예시

- @Mock 사용 (단위 테스트)
```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private JavaMailSender mailSender; // 단순 Mock
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private EmailService emailService; // 수동 주입
    
    @Test
    void testSendEmail() {
        // Spring 컨텍스트 없이 빠른 테스트
    }
}
```
- @MockBean 사용 (통합 테스트)
```java
@SpringBootTest
class EmailServiceIntegrationTest {
    
    @MockBean
    private JavaMailSender mailSender; // Spring 빈을 Mock으로 교체
    
    @Autowired
    private EmailService emailService; // 실제 Spring 빈 (Mock 주입됨)
    
    @Autowired
    private UserService userService; // 다른 실제 빈들과 함께 테스트
    
    @Test
    void testSendEmailWithRealDependencies() {
        // 실제 Spring 환경에서 테스트
    }
}
```
### 언제 어떤 것을 사용할까?
- @Mock 사용 시기
  - 빠른 단위 테스트가 필요할 때
  - 특정 클래스만 독립적으로 테스트할 때
  - Spring 컨텍스트가 필요 없을 때

- @MockBean 사용 시기
  - Spring 환경에서 통합 테스트할 때
  - 외부 서비스(메일, API 등)를 Mock으로 교체하고 싶을 때
  - 실제 Spring 빈들과의 상호작용을 테스트하고 싶을 때

간단히 말하면, @Mock은 순수 단위 테스트, @MockBean은 Spring과 함께하는 테스트입니다!

### Spring Boot 3.4.0부터 @MockBean이 deprecated되고, @MockitoBean으로 대체되었습니다.
`@MockitoBean`
```java
import org.springframework.test.context.bean.override.mockito.MockitoBean
```