### 기존 코드

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final MailProperties mailProperties;

    // 이메일 발송
    @Async
    public void sendEmail(User user, String emailToken) {
        try {
            String verificationUrl = buildVerificationUrl(emailToken);
            MimeMessage message = createVerificationEmailMessage(user.getEmail(), verificationUrl);
            sendEmailMessage(message, user.getEmail());
            createEmailVerification(user, emailToken);
        } catch (Exception e) {
            log.error("인증 이메일 발송 실패: {} - {}", user.getEmail(), e.getMessage());
            // TODO Exception 정의 필요
            throw new IllegalArgumentException("이메일 발송에 실패했습니다.");
        }
    }

    // 이메일 인증 완료
    @Transactional
    public void updateEmailVerification(EmailVerification emailVerification) {
        emailVerification.updateEmailVerification();
    }

    // Entity 생성 및 저장
    @Transactional
    private void createEmailVerification(User user, String token) {
        EmailVerification verification = EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(appProperties.getVerificationTokenExpiry()))
                .user(user)
                .build();

        emailVerificationRepository.save(verification);
    }

    public Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto parseTokenDto, String token, LocalDateTime now) {
        return emailVerificationRepository.findValidVerificationByParseToken(parseTokenDto, token, now);
    }

    // 이메일 인증 URL 생성
    private String buildVerificationUrl(String emailToken) {
        return appProperties.getFrontendUrl() + "/email-verifications/" + emailToken;
    }

    // 이메일 생성
    private MimeMessage createVerificationEmailMessage(String toEmail, String verificationUrl)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailProperties.getUsername());
        helper.setTo(toEmail);
        helper.setSubject("[오운완] 이메일 인증을 완료해주세요");
        helper.setText(buildVerificationEmailContent(verificationUrl), true);

        return message;
    }

    // 이메일 발송
    private void sendEmailMessage(MimeMessage message, String toEmail) {
        mailSender.send(message);
        log.info("인증 이메일 발송 완료: {}", toEmail);
    }

    // 이메일 내용 생성
    private String buildVerificationEmailContent(String verificationUrl) {
        return """
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;">
                <h2 style="color: #333;">오운완 이메일 인증</h2>
                <p>안녕하세요! 오운완 회원가입을 환영합니다.</p>
                <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" 
                       style="background-color: #007bff; color: white; padding: 12px 24px; 
                              text-decoration: none; border-radius: 5px; display: inline-block;">
                        이메일 인증하기
                    </a>
                </div>
                <p><strong>주의:</strong> 이 링크는 24시간 후 만료됩니다.</p>
                <p>링크가 작동하지 않는다면 아래 URL을 복사하여 브라우저에 붙여넣어주세요:</p>
                <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;">
                    %s
                </p>
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    이 이메일은 오운완 시스템에서 자동으로 발송된 메일입니다.
                </p>
            </div>
            """.formatted(verificationUrl, verificationUrl);
    }
}
```
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void signUp(CreateUserReq req) {
        try {
            User user = CreateUserReq.toEntity(req, passwordEncoder.encode(req.getPassword()));
            userService.create(user);

            String emailToken = tokenService.generateVerificationToken(user.getId(), user.getEmail(), VerificationType.SIGNUP);
            emailVerificationService.sendEmail(user, emailToken);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

    }

    // 이메일 인증
    public void verifyEmail(String token) {
        log.info("이메일 인증 시도: token={}", token);
        VerificationType type = VerificationType.SIGNUP;

        // 토큰을 풀어서 유효성 검사 필요 : User 이메일, VerificationType 확인 가능
        ParseTokenDto parseTokenDto = tokenService.validateToken(token, type);

        // 1. 토큰 유효성 검사
        EmailVerification verification = emailVerificationService
                .findValidVerificationByParseToken(parseTokenDto, token, LocalDateTime.now())
                // TODO Exception 설정
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 인증 링크입니다."));

        User user = verification.getUser();

        // 2. 사용자 상태 업데이트
        userService.updateUserStatusActive(user);

        // 3. 인증 완료 처리
        emailVerificationService.updateEmailVerification(verification);

        log.info("이메일 인증 완료: userId={}", user.getUserLoginId());
    }
}
```
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public void test() {
        log.info("TEST = {}", properties.getVerificationTokenExpiry());
    }

    // user 생성
    @Transactional
    public void create(User user) {
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatusActive(User user) {
        user.updateUserStatusActive();
    }
}
```

**고민한 내용**

- @Transactional을 원래 UserService와 EmailVerificationService의 메서드에만 붙였었는데, AuthFacade의 signup에서 이메일 인증 저장 등에 문제가 생기거나 했을 때 ROLLBACK이 안 됨 ⇒ 다른 트랜잭션에서 실행되기 때문
- @Async를 걸었는데도 응답까지 시간이 꽤 오래 걸림


**@Async가 하는 일**
- 비동기 실행: 메인 스레드를 블로킹하지 않고 별도 스레드에서 실행<br>
- 응답 속도 향상: 사용자는 이메일 발송 완료를 기다리지 않고 즉시 응답 받음<br>
- 장애 격리: 이메일 발송 실패가 회원가입 성공에 영향 주지 않음<br>

**해결 방법**

- 퍼사드에만 트랜잭션 붙이기
  - 여러 서비스가 사용될 때는 퍼사드에 트랜잭션, 서비스가 단일 호출될 때는 서비스에 트랜잭션(만약 두 경우가 다 있다면, 별도 메서드를 생성하거나 로직 정리 필요)
- 또, 현재 이메일 발송 메서드에서 Entity 저장까지 하는 형태라 발송이 완료 되어야지만 Entity 저장이 되어서 느림
- 이메일 발송 메서드(@Async)에서 @Transactional 메서드(EmailVerification Entity 저장 메서드) 분리하기 
  - 단순히 퍼사드에 @Transactional을 붙이는 것만으로는 안 되는 게 createEmailVerification이 @Async 메서드 내부에서 실행되면 메인 트랜잭션과 다른 스레드에서 실행되어 트랜잭션 참여 안됨
- 비동기 메서드가 트랜잭션에 참여되지 않게 구현 필요
```java
@Transactional
public void signUp(CreateUserReq req) {
    userService.create(user);                          // 트랜잭션 내
    emailVerificationService.sendEmail(user, token);  // @Async → 별도 스레드
    // createEmailVerification이 @Async 메서드 내부에서 실행되면
    // 메인 트랜잭션과 다른 스레드에서 실행되어 트랜잭션 참여 안됨!
}
```

### 최종 코드
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final MailProperties mailProperties;

    // 이메일 발송
    public void sendEmail(String email, String emailToken) {
        try {
            String verificationUrl = buildVerificationUrl(emailToken);
            MimeMessage message = createVerificationEmailMessage(email, verificationUrl);
            sendEmailMessage(message, email);
            // createEmailVerification 여기서 제거
        } catch (Exception e) {
            log.error("인증 이메일 발송 실패: {} - {}", email, e.getMessage());
            // TODO Exception 정의 필요
            throw new IllegalArgumentException("이메일 발송에 실패했습니다.");
        }
    }

    // 이메일 인증 완료
    public void updateEmailVerification(EmailVerification emailVerification) {
        emailVerification.updateEmailVerification();
    }

    // Entity 생성 및 저장
    public void createEmailVerification(User user, String token) {
        EmailVerification verification = EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(appProperties.getVerificationTokenExpiry()))
                .user(user)
                .build();

        emailVerificationRepository.save(verification);
    }

    public Optional<EmailVerification> findValidVerificationByParseToken(ParseTokenDto parseTokenDto, String token, LocalDateTime now) {
        return emailVerificationRepository.findValidVerificationByParseToken(parseTokenDto, token, now);
    }

    // 이메일 인증 URL 생성
    private String buildVerificationUrl(String emailToken) {
        return appProperties.getFrontendUrl() + "/email-verifications/" + emailToken;
    }

    // 이메일 생성
    private MimeMessage createVerificationEmailMessage(String toEmail, String verificationUrl)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailProperties.getUsername());
        helper.setTo(toEmail);
        helper.setSubject("[오운완] 이메일 인증을 완료해주세요");
        helper.setText(buildVerificationEmailContent(verificationUrl), true);

        return message;
    }

    // 이메일 발송
    private void sendEmailMessage(MimeMessage message, String toEmail) {
        mailSender.send(message);
        log.info("인증 이메일 발송 완료: {}", toEmail);
    }

    // 이메일 내용 생성
    private String buildVerificationEmailContent(String verificationUrl) {
        return """
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;">
                <h2 style="color: #333;">오운완 이메일 인증</h2>
                <p>안녕하세요! 오운완 회원가입을 환영합니다.</p>
                <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" 
                       style="background-color: #007bff; color: white; padding: 12px 24px; 
                              text-decoration: none; border-radius: 5px; display: inline-block;">
                        이메일 인증하기
                    </a>
                </div>
                <p><strong>주의:</strong> 이 링크는 24시간 후 만료됩니다.</p>
                <p>링크가 작동하지 않는다면 아래 URL을 복사하여 브라우저에 붙여넣어주세요:</p>
                <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;">
                    %s
                </p>
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    이 이메일은 오운완 시스템에서 자동으로 발송된 메일입니다.
                </p>
            </div>
            """.formatted(verificationUrl, verificationUrl);
    }
}
```
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 회원가입
    @Transactional
    public void signUp(CreateUserReq req) {
        User user = CreateUserReq.toEntity(req, passwordEncoder.encode(req.getPassword()));
        userService.create(user);

        String emailToken = tokenService.generateVerificationToken(user.getId(), user.getEmail(), VerificationType.SIGNUP);
        emailVerificationService.createEmailVerification(user, emailToken);

        applicationEventPublisher.publishEvent(new CreateUserEvent(user.getEmail(), emailToken));
    }

    // 이메일 인증
    public void verifyEmail(String token) {
        log.info("이메일 인증 시도: token={}", token);
        VerificationType type = VerificationType.SIGNUP;

        // 토큰을 풀어서 유효성 검사 필요 : User 이메일, VerificationType 확인 가능
        ParseTokenDto parseTokenDto = tokenService.validateToken(token, type);

        // 1. 토큰 유효성 검사
        EmailVerification verification = emailVerificationService
                .findValidVerificationByParseToken(parseTokenDto, token, LocalDateTime.now())
                // TODO Exception 설정
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 인증 링크입니다."));

        User user = verification.getUser();

        // 2. 사용자 상태 업데이트
        userService.updateUserStatusActive(user);

        // 3. 인증 완료 처리
        emailVerificationService.updateEmailVerification(verification);

        log.info("이메일 인증 완료: userId={}", user.getUserLoginId());
    }
}
```
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public void test() {
        log.info("TEST = {}", properties.getVerificationTokenExpiry());
    }

    // user 생성
    public void create(User user) {
        userRepository.save(user);
    }

    public void updateUserStatusActive(User user) {
        user.updateUserStatusActive();
    }
}
```
```java
@Component
@RequiredArgsConstructor
public class EmailEventListener {
    private final EmailVerificationService emailService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Async
    public void handleCreateUser(CreateUserEvent event) {
        emailService.sendEmail(event.getEmail(), event.getToken());
    }
}
```
```java
@Getter
@AllArgsConstructor
public class CreateUserEvent {
    private String email;
    private String token;
}
```
```java
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan(basePackages = "flobitt.oww")
@EnableAsync // 추가 필요
public class OwwApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwwApplication.class, args);
    }

}
```