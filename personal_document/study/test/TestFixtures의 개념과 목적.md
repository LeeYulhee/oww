### Test Fixture란?
- Fixture: "고정 장치"라는 뜻으로, 테스트에서 일관된 테스트 데이터를 제공하는 클래스
- 목적: 테스트 코드에서 반복적으로 사용되는 객체 생성 로직을 한 곳에 모아서 관리

왜 필요한가?
```java
// ❌ TestFixtures 없이 매번 반복
@Test
void test1() {
    User user = User.builder()
            .userLoginId("testuser")
            .email("test@example.com")
            .password("encodedPassword123!")
            .userStatus(UserStatus.NOT_VERIFIED)
            .build();
    // 테스트 로직...
}

@Test  
void test2() {
    User user = User.builder()  // 또 똑같은 코드 반복!
            .userLoginId("testuser2")
            .email("test2@example.com")
            .password("encodedPassword123!")
            .userStatus(UserStatus.NOT_VERIFIED)
            .build();
    // 테스트 로직...
}

// ✅ TestFixtures 사용
@Test
void test1() {
    User user = TestFixtures.createUser("test@example.com", "testuser");
    // 테스트 로직...
}

@Test
void test2() {
    User user = TestFixtures.createUser("test2@example.com", "testuser2");
    // 테스트 로직...
}
```

### 🚀 TestFixtures 사용의 장점
- 코드 중복 제거
    ```java
    // Before: 각 테스트마다 긴 객체 생성 코드
    // After: 한 줄로 간단하게
    User user = TestFixtures.createUser("test@example.com", "testuser");
    ```
- 유지보수성 향상
    ```java
    // User Entity에 새 필드가 추가되면 TestFixtures만 수정하면 됨
    // 모든 테스트가 자동으로 업데이트됨
    ```
- 테스트 가독성 향상
    ```java
    @Test
    void deleteExpiredUsers() {
        // given - 의도가 명확하게 드러남
        User expiredUser = TestFixtures.createUser("expired@example.com", "expired");
        User deletedUser = TestFixtures.createDeletedUser("deleted@example.com", "deleted");
        User activeUser = TestFixtures.createVerifiedUser("active@example.com", "active");
        
        // 테스트의 목적이 명확해짐
    }
    ```

- 일관성 보장
    ```java
    // 모든 테스트에서 동일한 방식으로 객체 생성
    // 실수로 잘못된 상태의 객체를 만들 가능성 줄어듦
    ```

### 실제 사용 패턴
```java
@Test
void completeUserFlow() {
    // 1. 회원가입
    User user = TestFixtures.createUser("test@example.com", "testuser");
    userRepository.save(user);
    
    // 2. 이메일 인증 정보 생성
    EmailVerification verification = TestFixtures.createEmailVerification(user, "token123");
    emailVerificationRepository.save(verification);
    
    // 3. 인증 처리
    authFacade.verifyEmail("token123");
    
    // 4. 인증된 사용자 확인
    User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(verifiedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
}
```