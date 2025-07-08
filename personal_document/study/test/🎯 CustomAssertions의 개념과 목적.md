### Custom Assertions란?
- AssertJ의 기본 assertion을 확장하여 도메인 특화적인 검증 로직을 제공하는 클래스
- 비즈니스 로직에 맞는 의미있는 검증 메서드를 제공해서 테스트의 가독성과 유지보수성을 향상

### 왜 필요한가?
```java
// ❌ 기본 AssertJ 사용 - 장황하고 의도가 불분명
@Test
void userShouldBeActive() {
    User user = userService.activateUser(userId);
    
    assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getEmailVerifiedAt()).isNotNull();
    assertThat(user.getIsDeleted()).isFalse();
}

// ✅ Custom Assertions 사용 - 간결하고 의도가 명확
@Test  
void userShouldBeActive() {
    User user = userService.activateUser(userId);
    
    assertThat(user).isActive(); // 한 줄로 모든 검증!
}
```

1. 진입점(Entry Point) 메서드들
```java
public static UserAssert assertThat(User actual) {
    return new UserAssert(actual);
}

public static EmailVerificationAssert assertThat(EmailVerification actual) {
    return new EmailVerificationAssert(actual);
}
```
특징:
- static import로 사용: import static CustomAssertions.assertThat;
- 메서드 오버로딩: 파라미터 타입에 따라 적절한 Assert 클래스 반환
- AssertJ 스타일 유지: 기존 assertThat() 사용법과 동일

사용법:
```java
import static flobitt.oww.CustomAssertions.assertThat;

@Test
void test() {
    User user = TestFixtures.createUser("test@example.com", "testuser");
    assertThat(user).hasEmail("test@example.com"); // CustomAssertions.assertThat() 호출
    
    EmailVerification verification = TestFixtures.createEmailVerification(user, "token");
    assertThat(verification).hasToken("token"); // CustomAssertions.assertThat() 호출
}
```

### 🏗️ 클래스 구조 분석
1. 진입점(Entry Point) 메서드들
```java
public static UserAssert assertThat(User actual) {
    return new UserAssert(actual);
}

public static EmailVerificationAssert assertThat(EmailVerification actual) {
    return new EmailVerificationAssert(actual);
}
```
특징:
- static import로 사용: import static CustomAssertions.assertThat;
- 메서드 오버로딩: 파라미터 타입에 따라 적절한 Assert 클래스 반환
- AssertJ 스타일 유지: 기존 assertThat() 사용법과 동일

사용법:
```java
import static flobitt.oww.CustomAssertions.assertThat;

@Test
void test() {
    User user = TestFixtures.createUser("test@example.com", "testuser");
    assertThat(user).hasEmail("test@example.com"); // CustomAssertions.assertThat() 호출
    
    EmailVerification verification = TestFixtures.createEmailVerification(user, "token");
    assertThat(verification).hasToken("token"); // CustomAssertions.assertThat() 호출
}
```

### 👤 UserAssert 클래스 상세 분석
클래스 선언
```java
public static class UserAssert extends AbstractAssert<UserAssert, User> {
```
특징:
- AbstractAssert 상속: AssertJ의 기본 기능들 상속받음 (isNotNull(), isEqualTo() 등)
- 제네릭 타입: <UserAssert, User> - 자기 자신 타입과 검증 대상 타입 명시
- Fluent Interface: 메서드 체이닝으로 연속적인 검증 가능

### 기본 검증 메서드들
1. 이메일 검증
```java
public UserAssert hasEmail(String email) {
    isNotNull();                                    // null 체크 먼저
    if (!actual.getEmail().equals(email)) {         // 실제 값과 기대값 비교
        failWithMessage("Expected user's email to be <%s> but was <%s>",
                email, actual.getEmail());          // 실패 시 명확한 메시지
    }
    return this;                                    // 메서드 체이닝을 위해 자기 자신 반환
}
```
사용 예시:
```java
@Test
void userShouldHaveCorrectEmail() {
    User user = TestFixtures.createUser("test@example.com", "testuser");
    
    assertThat(user).hasEmail("test@example.com");  // 성공
    assertThat(user).hasEmail("wrong@example.com"); // 실패: "Expected user's email to be <wrong@example.com> but was <test@example.com>"
}
```

2. 로그인 ID 검증
```java
public UserAssert hasLoginId(String loginId) {
    isNotNull();
    if (!actual.getUserLoginId().equals(loginId)) {
        failWithMessage("Expected user's login ID to be <%s> but was <%s>",
                loginId, actual.getUserLoginId());
    }
    return this;
}
```

3. 활성 상태 검증
```java
public UserAssert isActive() {
    isNotNull();
    if (actual.getUserStatus() != UserStatus.ACTIVE) {
        failWithMessage("Expected user to be ACTIVE but was <%s>",
                actual.getUserStatus());
    }
    if (actual.getEmailVerifiedAt() == null) {      // 추가 비즈니스 로직 검증
        failWithMessage("Expected user to have emailVerifiedAt but was null");
    }
    return this;
}
```
비즈니스 로직 포함:
- 단순히 userStatus == ACTIVE 확인뿐만 아니라
- emailVerifiedAt도 설정되어 있어야 한다는 비즈니스 규칙을 함께 검증

사용 예시:
```java
@Test
void userActivation_ShouldSetBothStatusAndVerificationTime() {
    // given
    User user = TestFixtures.createUser("test@example.com", "testuser");
    
    // when
    user.updateUserStatusActive();
    
    // then
    assertThat(user).isActive(); // 상태와 인증시간 모두 자동 검증!
}
```

4. 미인증 상태 검증
```java
public UserAssert isNotVerified() {
    isNotNull();
    if (actual.getUserStatus() != UserStatus.NOT_VERIFIED) {
        failWithMessage("Expected user to be NOT_VERIFIED but was <%s>",
                actual.getUserStatus());
    }
    return this;
}
```

5. 삭제 상태 검증
```java
public UserAssert isDeleted() {
    isNotNull();
    if (!actual.isDeleted()) {                      // isDeleted() 플래그 확인
        failWithMessage("Expected user to be deleted but was not");
    }
    if (actual.getDeletedAt() == null) {            // 삭제 시간도 함께 확인
        failWithMessage("Expected user to have deletedAt but was null");
    }
    return this;
}
```
Soft Delete 비즈니스 로직:
- isDeleted = true + deletedAt != null 을 모두 확인
- 비즈니스 규칙의 일관성 보장

### 🏗️ AssertJ의 상속 구조 이해

AbstractAssert의 구조
```java
// AssertJ 라이브러리의 AbstractAssert 클래스
public abstract class AbstractAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL> {
    
    protected final ACTUAL actual;    // 검증할 실제 객체
    private final Class<?> selfType;  // 자기 자신의 타입 정보
    
    protected AbstractAssert(ACTUAL actual, Class<?> selfType) {
        this.actual = actual;         // 검증 대상 저장
        this.selfType = selfType;     // 타입 정보 저장
    }
    
    // 이 메서드들이 제대로 동작하려면 생성자가 필요!
    public SELF isNotNull() { ... }
    public SELF isEqualTo(ACTUAL expected) { ... }
    protected void failWithMessage(String errorMessage, Object... arguments) { ... }
}
```

🎯 생성자가 하는 일

1. 부모 클래스 초기화
    ```java
    public UserAssert(User actual) {
        super(actual, UserAssert.class);  // 부모 생성자 호출
    }
    
    // 이것이 실행되면 AbstractAssert에서:
    // this.actual = actual;           // User 객체 저장
    // this.selfType = UserAssert.class;  // 타입 정보 저장
    ```

2. actual 필드 설정
    ```java
    // AbstractAssert의 actual 필드에 User 객체가 저장됨
    protected final ACTUAL actual;  // 여기에 우리가 전달한 User 객체가 들어감
    
    // 그래서 우리 메서드에서 actual을 사용할 수 있음
    public UserAssert hasEmail(String email) {
        if (!actual.getEmail().equals(email)) {  // ← actual 사용 가능!
            // ...
        }
        return this;
    }
    ```

3. 타입 정보 설정
    ```java
    // UserAssert.class 정보가 저장되어서
    // 메서드 체이닝이 올바른 타입으로 동작함
    assertThat(user)
        .hasEmail("test@example.com")  // UserAssert 반환
        .hasLoginId("testuser")        // UserAssert 반환  
        .isActive();                   // UserAssert 반환
    ```

**🔍 만약 생성자가 없다면?<br>**

컴파일 에러 발생
```java
public static class UserAssert extends AbstractAssert<UserAssert, User> {
    // 생성자 없음!
    
    public UserAssert hasEmail(String email) {
        // 컴파일 에러! actual을 사용할 수 없음
        if (!actual.getEmail().equals(email)) {  // ❌ actual이 초기화되지 않음
            // ...
        }
        return this;
    }
}
```
에러 메시지:
```java
There is no default constructor available in 'AbstractAssert'
```

왜 컴파일 에러가 날까?
```java
// AbstractAssert에는 기본 생성자가 없음!
public abstract class AbstractAssert<SELF, ACTUAL> {
    // ❌ 기본 생성자 없음
    
    // ✅ 매개변수가 있는 생성자만 존재
    protected AbstractAssert(ACTUAL actual, Class<?> selfType) {
        this.actual = actual;
        this.selfType = selfType;
    }
}

// 따라서 자식 클래스에서 반드시 super() 호출 필요!
```

### 🧩 생성자의 매개변수 분석
actual 매개변수
```java
public UserAssert(User actual) {
    super(actual, UserAssert.class);
    //    ^^^^^^ 검증할 User 객체
}
```
- 역할: 검증할 실제 객체를 받아서 부모 클래스에 전달
- 사용: 모든 검증 메서드에서 this.actual로 접근 가능

UserAssert.class 매개변수
```java
public UserAssert(User actual) {
    super(actual, UserAssert.class);
    //            ^^^^^^^^^^^^^^^^ 자기 자신의 타입 정보
}
```
- 역할: 메서드 체이닝에서 올바른 타입 반환을 위한 타입 정보 제공
- 사용: AssertJ 내부에서 타입 안전성 보장에 사용

### 🔄 실제 동작 과정
1. assertThat() 호출
```java
User user = TestFixtures.createUser("test@example.com", "testuser");
UserAssert userAssert = assertThat(user);  // new UserAssert(user) 실행
```

2. 생성자 실행
```java
public UserAssert(User actual) {
    super(actual, UserAssert.class);
    // AbstractAssert의 필드들이 초기화됨:
    // this.actual = user;  (전달받은 User 객체)
    // this.selfType = UserAssert.class;
}
```

3. 메서드 체이닝
```java
assertThat(user)                    // UserAssert 객체 생성
    .hasEmail("test@example.com")   // actual.getEmail() 사용 가능
    .hasLoginId("testuser")         // actual.getUserLoginId() 사용 가능
    .isNotVerified();              // actual.getUserStatus() 사용 가능
```