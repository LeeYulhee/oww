## 1. `given()` - 목(Mock) 객체의 동작 정의

```java
given(appProperties.getVerificationTokenExpiry()).willReturn(24);
```

- **의미**: "가짜 객체의 특정 메서드가 호출되면 미리 정의된 값을 반환하도록 설정"
- `appProperties.getVerificationTokenExpiry()`가 호출되면 `24`를 반환하도록 설정
- 실제 데이터베이스나 외부 시스템에 의존하지 않고 테스트 환경에서 원하는 값을 반환

## 2. `any()` - 매개변수 매처(Matcher)

```java
given(userRepository.findExpiredUnverifiedUsers(
        eq(UserStatus.NOT_VERIFIED), any(LocalDateTime.class)))
        .willReturn(Collections.emptyList());
```

- **의미**: "어떤 타입의 값이든 상관없이 매칭"
- `any(LocalDateTime.class)`: 어떤 LocalDateTime 객체가 와도 상관없음
- `eq(UserStatus.NOT_VERIFIED)`: 정확히 이 값과 일치해야 함
- 메서드의 특정 매개변수는 정확한 값을, 다른 매개변수는 타입만 맞으면 되는 경우 사용

## 3. `verify()` - 메서드 호출 검증

```java
verify(userRepository, times(1)).save(user);
```

- **의미**: "특정 메서드가 예상한 횟수만큼 호출되었는지 검증"
- `userRepository.save(user)`가 정확히 1번 호출되었는지 확인
- `times(1)`: 1번 호출되었는지 검증 (생략 가능, 기본값이 1)

## 4. `willReturn()`의 역할

```java
given(userRepository.findByEmailAndIsDeletedFalse(email))
        .willReturn(Optional.empty());
```

**의미**:

- `userRepository.findByEmailAndIsDeletedFalse(email)`이 호출되면
- 실제 데이터베이스에 가지 않고
- **`Optional.empty()`를 반환하도록 설정**

**다양한 반환값 설정 예시**

```java
// 1. 값 반환
given(userRepository.findById(1L)).willReturn(Optional.of(user));

// 2. 빈 Optional 반환
given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

// 3. 리스트 반환
given(userRepository.findAll()).willReturn(Arrays.asList(user1, user2));

// 4. 빈 리스트 반환
given(userRepository.findExpiredUsers()).willReturn(Collections.emptyList());

// 5. null 반환
given(someService.getSomeValue()).willReturn(null);

// 6. 기본값 반환
given(configService.getTimeout()).willReturn(30);
```

**예외 발생 시키기**

값을 반환하는 대신 예외를 던지게 할 수도 있습니다:

```java
// 예외 발생시키기
given(userRepository.findById(1L))
        .willThrow(new RuntimeException("Database error"));

// 또는
given(userRepository.save(any(User.class)))
        .willThrow(DataIntegrityViolationException.class);
```

**실제 테스트에서의 흐름**

```java
@Test
void testExample() {
// given - Mock 객체 동작 설정
    given(userRepository.findByEmailAndIsDeletedFalse("test@test.com"))
            .willReturn(Optional.empty());

// when - 실제 서비스 메서드 호출// 이때 userRepository.findByEmailAndIsDeletedFalse()가 호출되면// 실제 DB가 아닌 위에서 설정한 Optional.empty()가 반환됨

// then - 결과 검증// 서비스에서 Optional.empty()를 받았을 때 예외가 발생하는지 확인
}
```

**왜 이렇게 하나요?**

1. **실제 데이터베이스 없이 테스트**: DB 연결 없이도 테스트 가능
2. **예측 가능한 테스트**: 항상 같은 결과 반환
3. **빠른 테스트**: 실제 DB 조회보다 훨씬 빠름
4. **다양한 시나리오 테스트**: 존재하지 않는 데이터, 예외 상황 등을 쉽게 테스트

**요약**: `willReturn()`은 "이 메서드가 호출되면 이 값을 반환해줘"라고 Mock 객체에게 지시하는 것입니다.

## 전체 테스트 구조 (Given-When-Then)

```java
@Test
void testExample() {
// given (준비) - 테스트 조건 설정
    given(mockObject.method()).willReturn(expectedValue);

// when (실행) - 테스트할 메서드 호출
    Result result = serviceUnderTest.methodToTest();

// then (검증) - 결과 확인
    assertThat(result).isEqualTo(expectedResult);
    verify(mockObject).method();// 메서드가 호출되었는지 확인

```