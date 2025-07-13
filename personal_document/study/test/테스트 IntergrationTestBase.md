```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class IntegrationTestBase {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.6")
            .withDatabaseName("oww_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }
}
```

### 🏗️ 클래스 선언부
`public abstract class IntegrationTestBase`

- abstract 키워드: 이 클래스는 직접 인스턴스화할 수 없고, 다른 테스트 클래스들이 상속받아 사용하는 기반 클래스입니다
- 역할: 통합 테스트에 필요한 공통 설정을 제공하는 베이스 클래스

### 📋 어노테이션들
`@SpringBootTest`
```java
@SpringBootTest
```
- 역할: Spring Boot 애플리케이션 전체 컨텍스트를 로드합니다
- 동작:
  - 실제 애플리케이션이 시작되는 것처럼 모든 Bean들을 생성하고 의존성을 주입합니다
  - @Component, @Service, @Repository, @Controller 등 모든 Bean이 등록됩니다
  - 실제 환경과 가장 유사한 테스트 환경을 제공합니다



`@ActiveProfiles("test")`
```java
@ActiveProfiles("test")
```
- 역할: Spring의 test 프로파일을 활성화합니다
- 동작:
  - application-test.yml 또는 application-test.properties 파일의 설정을 사용합니다
  - 운영 환경과 다른 테스트 전용 설정을 적용할 수 있습니다
  - 예: 테스트용 데이터베이스, 로깅 레벨, 외부 서비스 모킹 등

`@Testcontainers`
```java
@Testcontainers
```
- 역할: TestContainers 라이브러리를 활성화합니다
- 동작:
  - Docker 컨테이너를 자동으로 관리합니다
  - @Container 어노테이션이 붙은 컨테이너들을 테스트 시작 전에 실행하고, 테스트 완료 후 정리합니다
  - 실제 데이터베이스 환경에서 테스트할 수 있게 해줍니다

`@Transactional`
```java
@Transactional
```
- 역할: 각 테스트 메서드를 트랜잭션으로 감쌉니다
- 동작:
  - 테스트 메서드 실행 후 자동으로 롤백됩니다
  - 테스트 간 데이터 격리를 보장합니다 (한 테스트의 데이터가 다른 테스트에 영향을 주지 않음)
  - 데이터베이스 정리 작업을 자동화합니다


`@TestPropertySource`
```java
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.scheduler.enabled=false"
})
```
- 역할: 테스트 전용 프로퍼티를 설정합니다
- 각 프로퍼티 설명:
  - `spring.jpa.hibernate.ddl-auto=create-drop:`
    - 테스트 시작 시 테이블을 생성하고, 테스트 완료 시 테이블을 삭제합니다
    - 깨끗한 데이터베이스 상태로 각 테스트를 실행합니다
  - `app.scheduler.enabled=false:`
    - 스케줄러 기능을 비활성화합니다
    - 테스트 중에 백그라운드 스케줄러가 실행되어 테스트를 방해하는 것을 방지합니다


### 🐳 TestContainers 설정
`@Container static MariaDBContainer<?> mariaDB`
```java
@Container
static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.6")
        .withDatabaseName("oww_test")
        .withUsername("test")
        .withPassword("test");
```
- `@Container`: TestContainers가 이 컨테이너를 관리하도록 지정합니다
- `static`: 모든 테스트 클래스에서 동일한 컨테이너 인스턴스를 공유합니다
- `MariaDBContainer<>("mariadb:10.6")`: MariaDB 10.6 버전의 Docker 이미지를 사용합니다
- 설정 메서드들:
  - `.withDatabaseName("oww_test")`: 데이터베이스 이름을 'oww_test'로 설정
  - `.withUsername("test")`: 사용자명을 'test'로 설정
  - `.withPassword("test")`: 비밀번호를 'test'로 설정



`@DynamicPropertySource` 블록
```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
    registry.add("spring.datasource.username", mariaDB::getUsername);
    registry.add("spring.datasource.password", mariaDB::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
}
```
📋 전체 구조 설명
- 실행 시점: Spring Context가 시작되기 직전에 실행됩니다 (컨테이너 시작 후, Bean 생성 전)
- 실행 빈도: 테스트 클래스당 한 번만 실행됩니다
- 목적: Spring의 Environment에 동적 프로퍼티를 등록합니다
- 각 라인 설명:
  - @DynamicPropertySource: 이 메서드가 동적 프로퍼티 설정용임을 Spring에 알립니다(Spring Boot가 Context 생성 전에 이 메서드를 자동으로 호출)
    - 메서드는 반드시 static이어야 하고, DynamicPropertyRegistry 파라미터를 받아야 합니다
  - static void configureProperties(DynamicPropertyRegistry registry):
    - static: 클래스 인스턴스 생성 전에 실행되어야 하므로 static 필수
    - DynamicPropertyRegistry registry: Spring이 제공하는 프로퍼티 등록 인터페이스
    - 메서드명: 임의로 정할 수 있음 (configureProperties, setProperties 등)
  - registry.add(): Spring Environment에 프로퍼티를 추가하는 메서드
- 전체 실행 순서
    ```plantuml
    1. 클래스 로딩
       ↓
    2. @Container 컨테이너들 시작 ⬅️ @Testcontainers가 관리
       ↓
    3. @DynamicPropertySource 메서드 실행 ⬅️ 여기서 프로퍼티 등록!
       ↓
    4. Spring Context 생성 (등록된 프로퍼티 사용)
       ↓
    5. Bean 초기화 (DataSource 등)
       ↓
    6. 테스트 실행
       ↓
    7. 테스트 완료
       ↓
    8. 컨테이너 정리
    ```
    ```plantuml
        1. 테스트용 MariaDB 컨테이너 생성 🐳
         ↓
        2. 컨테이너가 시작되면서 랜덤 포트 할당 🎲
         ↓
        3. @DynamicPropertySource가 컨테이너 정보를 가져옴 📡
         ↓
        4. Spring이 그 정보로 실제 DB 연결 🌱
    ```

### 🔄 전체 동작 과정
1. 클래스 로드 시
```java
static {
    mariaDB.start(); // Docker에서 MariaDB 컨테이너 시작
    // Spring의 DataSource 설정을 동적으로 구성
}
```
2. 테스트 실행 시
```java
@SpringBootTest // 전체 Spring 컨텍스트 로드
@ActiveProfiles("test") // test 프로파일 활성화
@Testcontainers // 컨테이너 생명주기 관리
```
3. 각 테스트 메서드 실행 시
```java
@Transactional // 트랜잭션 시작
// 테스트 실행
// 트랜잭션 롤백 (데이터 정리)
```
4. 테스트 완료 시
```java
@Testcontainers // 컨테이너 자동 정리
```

### 📊 실제 사용 예시
```java
// 이 클래스를 상속받는 테스트 클래스
class UserServiceIntegrationTest extends IntegrationTestBase {
    
    @Autowired
    private UserService userService; // 실제 Bean 주입
    
    @Autowired
    private UserRepository userRepository; // 실제 Repository
    
    @Test
    void createUser_Success() {
        // 실제 MariaDB에 데이터 저장/조회 테스트
        User user = new User(...);
        userService.createUser(user);
        
        // 실제 데이터베이스에서 조회
        User saved = userRepository.findById(user.getId());
        assertThat(saved).isNotNull();
        
        // 테스트 완료 후 @Transactional에 의해 자동 롤백
    }
}
```

### 🎯 이 설정의 장점
1. 실제 환경과 유사: 실제 MariaDB를 사용하여 테스트
2. 격리된 환경: 각 테스트가 독립적으로 실행
3. 자동 정리: 컨테이너와 데이터가 자동으로 정리됨
4. 재사용성: 여러 통합 테스트 클래스에서 공통으로 사용 가능
5. 일관성: 모든 개발자가 동일한 테스트 환경을 사용

### application-test.yml
```yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  mail:
    host: localhost
    port: 25
    username: test@test.com
    password: test

app:
  frontend-url: http://localhost:8080
  verification-token-expiry: 24
  hard-delete-days: 7
  scheduler:
    enabled: false

jwt:
  verification-key: test-secret-key-for-verification-tokens-must-be-long-enough

logging:
  level:
    flobitt.oww: DEBUG
```

### Test DB
```java
@Container
static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.6")
        .withDatabaseName("oww_test")
        .withUsername("test")
        .withPassword("test");
```
이 코드가 실행되면 TestContainers가:
1. Docker에서 MariaDB 컨테이너 실행
2. 자동으로 root 계정과 데이터베이스 생성
3. 기본 사용자 계정 생성 (username: "test", password: "test")
4. 지정한 데이터베이스 생성 ("oww_test")
5. 랜덤 포트로 접근 가능하게 설정

실제로 생성되는 것들
```bash
# TestContainers가 자동으로 실행하는 것과 동일
docker run -d \
  -e MYSQL_ROOT_PASSWORD=test \
  -e MYSQL_DATABASE=oww_test \
  -e MYSQL_USER=test \
  -e MYSQL_PASSWORD=test \
  -p 랜덤포트:3306 \
  mariadb:10.6
```

### 🔄 전체 과정 시각화
```plantuml
// 🐳 TestContainers가 하는 일
MariaDB Container 생성
├── Docker에서 MariaDB 실행
├── 랜덤 포트 할당 (예: 32768)
├── 데이터베이스 'oww_test' 생성
└── 사용자 'test/test' 생성

// 📡 @DynamicPropertySource가 하는 일  
컨테이너 정보 수집
├── JDBC URL: jdbc:mariadb://localhost:32768/oww_test
├── Username: test
└── Password: test

// 🌱 Spring이 하는 일
DataSource 설정
├── 위 정보로 DataSource Bean 생성
├── JPA EntityManager 설정
└── Repository들이 실제 DB에 연결

// 🧪 테스트에서 하는 일
실제 DB 사용
├── userRepository.save(user) → 실제 MariaDB에 저장
├── userRepository.findById(id) → 실제 MariaDB에서 조회
└── @Transactional로 테스트 후 롤백
```