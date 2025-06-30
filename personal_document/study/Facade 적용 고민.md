### UserService와 EmailVerificationService 구조 고민
1. UserService에서 회원가입 후에 메일을 바로 발송하면 EmailVerificationService를 사용하게 되는데, EmailVerificationService에서는 토큰 인증 후에 userStatus를 업데이트 해줘야 해서 UserService를 참조하니 순환참조가 일어남.
2. 다음 차선책은 Controller에서 userService와 emailService를 전부 호출하는 건데 이렇게 되면 Controller에서 비즈니스 로직을 알게 되는 관점이 아닌지


**방법 1: 이벤트 기반 아키텍처(권장)**

장점: 완전한 디커플링, 확장성 좋음<br>
단점: 복잡도 약간 증가
```java
// 1. 이벤트 정의
@Getter @AllArgsConstructor
public class UserCreatedEvent {
    private String userId;
    private String email;
}

@Getter @AllArgsConstructor
public class EmailVerifiedEvent {
    private String userId;
    private String email;
}

// 2. UserService - 이벤트 발행만
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;  // 이벤트 발행
    
    public SignupResponse signup(SignupRequest request) {
        // 사용자 생성
        User user = createUser(request);
        
        // 이벤트 발행 (EmailVerificationService가 처리)
        eventPublisher.publishEvent(new UserCreatedEvent(user.getUserId(), user.getEmail()));
        
        return SignupResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .message("회원가입이 완료되었습니다.")
                .build();
    }
    
    public void activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        
        user.setUserStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 이벤트 발행 (환영 메일 등)
        eventPublisher.publishEvent(new EmailVerifiedEvent(userId, user.getEmail()));
        
        log.info("사용자 계정 활성화: userId={}", userId);
    }
}

// 3. EmailVerificationService - 이벤트 리스너
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    
    private final EmailService emailService;
    private final TokenService tokenService;
    private final UserService userService;  // 이제 단방향 참조
    private final AppProperties appProperties;
    
    @EventListener
    @Async
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("사용자 생성 이벤트 처리: userId={}", event.getUserId());
        sendVerificationEmail(event.getUserId(), event.getEmail());
    }
    
    @EventListener
    @Async
    public void handleEmailVerified(EmailVerifiedEvent event) {
        log.info("이메일 인증 완료 이벤트 처리: userId={}", event.getUserId());
        // 환영 메일 발송
        try {
            emailService.sendWelcomeEmail(event.getEmail(), "사용자");
        } catch (Exception e) {
            log.warn("환영 메일 발송 실패: userId={}", event.getUserId(), e);
        }
    }
    
    public void verifyEmail(String token) {
        // 토큰 검증
        VerificationResult result = validateToken(token);
        if (!result.isValid()) {
            throw new InvalidTokenException(result.getErrorMessage());
        }
        
        // UserService에 계정 활성화 요청 (단방향)
        userService.activateUser(result.getUserId());
    }
}

// 4. Controller - 단순화
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = userService.signup(request);  // 이벤트가 자동 처리
        return ResponseEntity.ok(ApiResponse.success("회원가입 완료", response));
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);  // 이벤트가 자동 처리
        return ResponseEntity.ok(ApiResponse.success("인증 완료"));
    }
}
```

**방법 2: 퍼사드 패턴**

장점: 단순함, 이해하기 쉬움<br>
단점: 퍼사드가 커질 수 있음
```java
// AuthFacade - 회원가입/인증 관련 모든 오케스트레이션
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthFacade {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    
    public SignupResponse signup(SignupRequest request) {
        // 1. 사용자 생성 (UserService)
        User user = userService.createUser(request);
        
        // 2. 이메일 발송 (EmailVerificationService)
        emailVerificationService.sendVerificationEmail(user.getUserId(), user.getEmail());
        
        return SignupResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .message("회원가입이 완료되었습니다.")
                .build();
    }
    
    public void verifyEmail(String token) {
        // 1. 토큰 검증 (EmailVerificationService)
        EmailVerificationService.VerificationResult result = 
            emailVerificationService.validateToken(token);
        
        if (!result.isValid()) {
            throw new InvalidTokenException(result.getErrorMessage());
        }
        
        // 2. 계정 활성화 (UserService)
        userService.activateUser(result.getUserId());
        
        // 3. 환영 메일 발송 (EmailVerificationService)
        emailVerificationService.sendWelcomeEmail(result.getEmail());
    }
    
    public void resendVerificationEmail(String email) {
        // 1. 사용자 조회 (UserService)
        User user = userService.findByEmailForVerification(email);
        
        // 2. 재발송 (EmailVerificationService)
        emailVerificationService.sendVerificationEmail(user.getUserId(), user.getEmail());
        
        // 3. 마지막 활동 시간 업데이트 (UserService)
        userService.updateLastActivity(user);
    }
}

// Controller에서는 Facade만 사용
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;
    private final UserService userService;  // 로그인용
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authFacade.signup(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입 완료", response));
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authFacade.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("인증 완료"));
    }
}
```

**방법 3: 인터페이스 분리(DIP 적용)**

장점: 의존성 역전, 테스트 용이<br>
단점: 인터페이스 관리 필요
```java
// 1. 인터페이스 정의
public interface UserActivator {
void activateUser(String userId);
User findUserForVerification(String userId);
}

public interface EmailSender {
void sendVerificationEmail(String userId, String email);
}

// 2. UserService - 인터페이스 구현
@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserActivator {

    private final UserRepository userRepository;
    
    @Override
    public void activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        
        user.setUserStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Override
    public User findUserForVerification(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}

// 3. EmailVerificationService - 인터페이스 의존
@Service
@RequiredArgsConstructor
public class EmailVerificationService implements EmailSender {
    
    private final EmailService emailService;
    private final TokenService tokenService;
    private final UserActivator userActivator;  // 인터페이스 의존
    
    public void verifyEmail(String token) {
        VerificationResult result = validateToken(token);
        if (!result.isValid()) {
            throw new InvalidTokenException(result.getErrorMessage());
        }
        
        // 인터페이스를 통한 호출
        userActivator.activateUser(result.getUserId());
    }
    
    @Override
    public void sendVerificationEmail(String userId, String email) {
        // 구현
    }
}

// 4. AuthService - 두 인터페이스 조합
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserService userService;
    private final EmailSender emailSender;  // 인터페이스 의존
    
    public SignupResponse signup(SignupRequest request) {
        User user = userService.createUser(request);
        emailSender.sendVerificationEmail(user.getUserId(), user.getEmail());
        // ...
    }
}
```

**방법 4: Controller 오케스트레이션**

장점: 단순하고 직관적<br>
단점: Controller가 비즈니스 로직 알게 됨<br>
(⇒ Controller의 본래 책임은 HTTP 요청/응답 처리, '어떻게 처리할지'가 아니라 '무엇을 처리할지'만 알아야 함)
```java
@RestController
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        // 1. 사용자 생성
        User user = userService.createUser(request);
        
        // 2. 이메일 발송
        emailVerificationService.sendVerificationEmail(user.getUserId(), user.getEmail());
        
        SignupResponse response = SignupResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .message("회원가입이 완료되었습니다.")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("회원가입 완료", response));
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        // 1. 토큰 검증
        EmailVerificationService.VerificationResult result = 
            emailVerificationService.validateToken(token);
        
        if (!result.isValid()) {
            throw new InvalidTokenException(result.getErrorMessage());
        }
        
        // 2. 계정 활성화
        userService.activateUser(result.getUserId());
        
        // 3. 환영 메일 (선택적)
        emailVerificationService.sendWelcomeEmail(result.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success("인증 완료"));
    }
}
```

**🎯 권장사항**

**1순위: 이벤트 기반 아키텍처<br>**
완전한 디커플링<br>
확장성 좋음 (새로운 이벤트 리스너 쉽게 추가)<br>
Spring의 @EventListener 활용

**2순위: 퍼사드 패턴<br>**
이해하기 쉬움<br>
순환참조 해결<br>
비즈니스 로직이 한 곳에 집중

**3순위: Controller 오케스트레이션<br>**
가장 단순함<br>
작은 프로젝트에서는 충분

**⇒ 결론 : 도메인이 같으면 퍼사드 패턴, 도메인이 다르면 이벤트 기반으로 구현하기**

---