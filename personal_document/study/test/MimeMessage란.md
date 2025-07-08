## MimeMessage란?

`MimeMessage`는 JavaMail API에서 제공하는 클래스로, **이메일 메시지를 표현하는 객체**입니다.

```java
// 실제 서비스에서 사용하는 부분
MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

helper.setFrom(mailProperties.getUsername());
helper.setTo(toEmail);
helper.setSubject("[오운완] 이메일 인증을 완료해주세요");
helper.setText(buildVerificationEmailContent(verificationUrl), true);

```

### MimeMessage의 역할:

- **발신자, 수신자 정보** 저장
- **제목, 본문 내용** 저장
- **첨부파일, HTML 내용** 등 처리
- **MIME 형식**으로 이메일 구조화

## Session이란?

`Session`은 JavaMail에서 **메일 서버와의 연결 설정 정보를 담는 객체**입니다.

```java
// 실제 운영에서는 이런 식으로 생성됩니다
Properties props = new Properties();
props.put("mail.smtp.host", "smtp.gmail.com");
props.put("mail.smtp.port", "587");
props.put("mail.smtp.auth", "true");
Session session = Session.getInstance(props, authenticator);

```

### Session의 역할:

- **SMTP 서버 정보** (host, port)
- **인증 정보** (username, password)
- **SSL/TLS 설정**
- **연결 타임아웃** 등의 설정

## 테스트에서 왜 Mock을 사용하는가?

# 이메일 테스트에서 Mock을 사용하는 이유

## 1. 실제 이메일 발송을 피하기 위해

```java
// ❌ 실제로 이메일이 발송되면 문제가 됨
@Test
void sendEmail_WithoutMock() {
    // 이렇게 하면 실제로 test@example.com으로 이메일이 발송됨!
    emailVerificationService.sendEmail("test@example.com", "token");
}

// ✅ Mock을 사용하여 실제 발송 차단
@Test
void sendEmail_WithMock() {
    doNothing().when(mailSender).send(any(MimeMessage.class));
    // 실제 발송 없이 로직만 테스트
    emailVerificationService.sendEmail("test@example.com", "token");
}

```

## 2. 외부 의존성 제거

```java
// MimeMessage는 Session 객체가 필요함
// Session은 실제 SMTP 서버 연결 정보가 필요함
void setMimeSession() {
    // 가짜 Session으로 MimeMessage 생성 가능하게 함
    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mockMimeMessage = new MimeMessage(session);

    given(mailSender.createMimeMessage()).willReturn(mockMimeMessage);
}

```

## 3. 테스트 속도 향상

```java
// 실제 SMTP 연결 시간 (수 초)을 Mock으로 즉시 처리
given(mailSender.createMimeMessage()).willReturn(mockMimeMessage); // 즉시 반환
doNothing().when(mailSender).send(any(MimeMessage.class)); // 즉시 완료

```

## 4. 테스트 환경 독립성

```java
// 네트워크 상태, SMTP 서버 상태와 무관하게 테스트 가능
@Test
void sendEmail_Success() {
    // SMTP 서버가 다운되어도 이 테스트는 성공함
    setMimeSession();
    doNothing().when(mailSender).send(any(MimeMessage.class));

    emailVerificationService.sendEmail("test@example.com", "token");

    verify(mailSender).send(any(MimeMessage.class)); // 호출 여부만 검증
}

```

## 5. 예외 상황 시뮬레이션

```java
@Test
void sendEmail_Failure_ThrowsException() {
    setMimeSession();
    // SMTP 서버 에러 상황을 인위적으로 만들어 테스트
    doThrow(new RuntimeException("SMTP server error"))
            .when(mailSender).send(any(MimeMessage.class));

    assertThatThrownBy(() -> emailVerificationService.sendEmail("test@example.com", "token"))
            .isInstanceOf(IllegalArgumentException.class);
}

```

## 실제 vs Mock 비교

| 구분 | 실제 이메일 발송 | Mock 사용 |
| --- | --- | --- |
| **속도** | 느림 (수 초) | 빠름 (밀리초) |
| **비용** | SMTP 서버 비용 | 무료 |
| **부작용** | 실제 이메일 발송 | 없음 |
| **안정성** | 네트워크/서버 의존 | 항상 안정 |
| **테스트 목적** | 통합 테스트 | 단위 테스트 |

# doNothing() 메서드의 역할

## 1. 실제 이메일 발송 차단

```java
// 실제 서비스 코드에서 이 부분이 실행됨
private void sendEmailMessage(MimeMessage message, String toEmail) {
    mailSender.send(message);  // ← 이 부분이 실제로는 이메일을 발송함
    log.info("인증 이메일 발송 완료: {}", toEmail);
}

```

```java
// ❌ doNothing() 없으면 어떻게 될까?
@Test
void sendEmail_WithoutDoNothing() {
    setMimeSession();
    // doNothing() 설정 없음

    // 실제로 이메일이 발송되거나 에러 발생!
    emailVerificationService.sendEmail("test@example.com", "token");
}

// ✅ doNothing()으로 실제 발송 차단
@Test
void sendEmail_WithDoNothing() {
    setMimeSession();
    doNothing().when(mailSender).send(any(MimeMessage.class)); // 실제 발송 차단

    // 안전하게 테스트 가능
    emailVerificationService.sendEmail("test@example.com", "token");
}

```

## 2. void 메서드 모킹

```java
// mailSender.send()는 반환값이 없는 void 메서드
public interface JavaMailSender {
    void send(MimeMessage mimeMessage) throws MailException;
    //   ↑ void 반환타입
}

// void 메서드는 given().willReturn() 사용 불가
// given(mailSender.send(any())).willReturn(???); // ❌ 컴파일 에러

// void 메서드는 doNothing(), doThrow() 등을 사용
doNothing().when(mailSender).send(any(MimeMessage.class)); // ✅ 올바른 방법

```

## 3. 다른 동작 패턴들

```java
// 1. 아무것도 하지 않기 (성공 케이스)
doNothing().when(mailSender).send(any(MimeMessage.class));

// 2. 예외 발생시키기 (실패 케이스)
doThrow(new RuntimeException("SMTP server error"))
    .when(mailSender).send(any(MimeMessage.class));

// 3. 특정 동작 수행하기
doAnswer(invocation -> {
    MimeMessage message = invocation.getArgument(0);
    System.out.println("이메일 발송됨: " + message.getSubject());
    return null;
}).when(mailSender).send(any(MimeMessage.class));

```

## 4. 실제 테스트 코드에서의 흐름

```java
@Test
@DisplayName("이메일 발송 성공")
void sendEmail_Success() {
    // given
    String email = "test@example.com";
    String token = "verification-token";
    setMimeSession();  // MimeMessage 생성 준비
    doNothing().when(mailSender).send(any(MimeMessage.class)); // 실제 발송 차단

    // when
    emailVerificationService.sendEmail(email, token);
    // ↓ 내부적으로 실행되는 과정
    // 1. buildVerificationUrl(token) 호출
    // 2. createVerificationEmailMessage(email, url) 호출
    // 3. sendEmailMessage(message, email) 호출
    //    └─ mailSender.send(message) 호출 ← doNothing()으로 차단됨

    // then
    verify(mailSender, times(1)).send(any(MimeMessage.class)); // 호출 여부만 검증
    verify(mailSender, times(1)).createMimeMessage();
}

```

## 5. 테스트 목적: "행위 검증"

```java
// 우리가 테스트하고 싶은 것
// ✅ 이메일 발송 로직이 올바르게 실행되는가?
// ✅ mailSender.send()가 정확히 1번 호출되는가?
// ✅ 올바른 파라미터로 호출되는가?

// 우리가 테스트하고 싶지 않은 것
// ❌ 실제 SMTP 서버와 연결되는가?
// ❌ 실제 이메일이 전송되는가?
// ❌ 네트워크 상태는 정상인가?

verify(mailSender, times(1)).send(any(MimeMessage.class));
// ↑ "send 메서드가 1번 호출되었는지"만 검증

```

## 요약

`doNothing()`의 핵심 역할:

1. **실제 이메일 발송 방지** - 테스트 환경에서 실제 이메일이 나가지 않음
2. **void 메서드 모킹** - 반환값이 없는 메서드를 안전하게 모킹
3. **외부 의존성 제거** - SMTP 서버 없이도 테스트 가능
4. **행위 검증 가능** - 메서드 호출 여부와 횟수만 검증

## 간단한 비유

음식점에서 요리사가 "음식을 손님 테이블로 가져다 주세요"라고 서빙 직원에게 말하는 상황을 생각해보세요.

```java
// 실제 운영 환경
서빙직원.음식전달(음식);  // 실제로 손님 테이블에 음식을 가져다 줌

// 테스트 환경  
doNothing().when(서빙직원).음식전달(any(음식.class));
서빙직원.음식전달(음식);  // 실제로는 음식을 가져다 주지 않음 (가짜 행동)

// 그 대신 이것만 확인
verify(서빙직원).음식전달(any(음식.class));  // "음식전달 지시가 내려졌나?"만 확인
```

**테스트의 목적**: "요리사가 서빙 직원에게 올바르게 지시를 내렸는가?"를 확인하는 것이지, "실제로 음식이 손님에게 전달되었는가?"를 확인하는 것이 아닙니다.

마찬가지로 이메일 테스트에서는 "이메일 발송 로직이 올바르게 작동하는가?"만 확인하고, 실제 이메일 발송은 차단하는 것입니다.