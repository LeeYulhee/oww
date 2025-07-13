### 문제 상황

---

- 전체 테스트 실행 시 아래 오류 발생

    ```java
    2025-07-13T05:08:23.192+09:00  WARN 27236 --- [    Test worker] com.zaxxer.hikari.pool.PoolBase          : HikariPool-3 - Failed to validate connection org.mariadb.jdbc.Connection@6228deac ((conn=5) Connection.setNetworkTimeout cannot be called on a closed connection). Possibly consider using a shorter maxLifetime value.
    2025-07-13T05:08:23.193+09:00  WARN 27236 --- [    Test worker] com.zaxxer.hikari.pool.PoolBase          : HikariPool-3 - Failed to validate connection org.mariadb.jdbc.Connection@6b993d8b ((conn=4) Connection.setNetworkTimeout cannot be called on a closed connection). Possibly consider using a shorter maxLifetime value.
    2025-07-13T05:08:43.201+09:00  WARN 27236 --- [    Test worker] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08000
    2025-07-13T05:08:43.201+09:00 ERROR 27236 --- [    Test worker] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-3 - Connection is not available, request timed out after 20007ms (total=0, active=0, idle=0, waiting=0)
    2025-07-13T05:08:43.201+09:00 ERROR 27236 --- [    Test worker] o.h.engine.jdbc.spi.SqlExceptionHelper   : Socket fail to connect to localhost:2065. Connection refused: getsockopt
    2025-07-13T05:08:43.202+09:00  WARN 27236 --- [    Test worker] o.s.test.context.TestContextManager      : Caught exception while invoking 'beforeTestMethod' callback on TestExecutionListener [org.springframework.test.context.transaction.TransactionalTestExecutionListener] for test method [void flobitt.oww.intergration.UserCleanupSchedulerIntegrationTest.hardDeleteExpiredDeletedUsersIntegration() throws java.lang.Exception] and test instance [flobitt.oww.intergration.UserCleanupSchedulerIntegrationTest@7467b8ee]
    
    org.springframework.transaction.CannotCreateTransactionException: Could not open JPA EntityManager for transaction
    ```


### 문제 발생한 테스트 코드

---

- IntegrationTestBase를 상속한 통합 테스트 클래스 구조

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("integration-test")
    @Testcontainers
    @Transactional
    @Import(TestConfig.class)
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


### 시도해본 해결 방법

---

- HikariCP 관련 연결 풀 설정 추가

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("integration-test")
    @Testcontainers
    @Transactional
    @Import(TestConfig.class)
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
    
            // HikariCP 연결 풀 설정 추가
            registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
            registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
            registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
            registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
            registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
        }
    }
    ```

- @Sql 또는 @AfterEach를 통해 테스트 간 Repository 데이터 초기화
- 트랜잭션 누락/미반환 의심으로 관련 설정 점검
- withReuse(true) 설정 추가로 컨테이너 재사용 시도

### 테스트 클래스 목록 및 실행 순서

---

- 테스트 순서
    1. `UserSignupIntegrationTest`  : `@AutoConfigureMockMvc` 있음

        ```java
        /**
         * 사용자 가입부터 이메일 인증까지의 전체 플로우 통합 테스트
         */
        @AutoConfigureMockMvc
        class UserSignupIntegrationTest extends IntegrationTestBase {
        	...
        }
        ```

    2. `UserCleanupSchedulerIntegrationTest`  : `@AutoConfigureMockMvc` 없음

        ```java
        /**
         * 사용자 정리 스케줄러 통합 테스트
         */
        class UserCleanupSchedulerIntegrationTest extends IntegrationTestBase {
        	...
        }
        ```

    3. `ValidationIntegrationTest`  : `@AutoConfigureMockMvc` 있음

        ```java
        /**
         * 유효성 검사 통합 테스트
         */
        @AutoConfigureMockMvc
        class ValidationIntegrationTest extends IntegrationTestBase {
        	...
        }
        ```


### 원인 분석

---

**1️⃣ 컨텍스트 분리 발생**

- `UserSignupIntegrationTest`와 `ValidationIntegrationTest`는 유사한 설정(`@AutoConfigureMockMvc` 포함)으로 인해 같은 `ApplicationContext(컨텍스트 A)` 를 재사용
- 반면, `UserCleanupSchedulerIntegrationTest`는 설정이 달라서 다른 `ApplicationContext(컨텍스트 B)` 가 생성됨

**2️⃣ DB 컨테이너 재생성 및 포트 변경**

- `@Container`로 정의된 Testcontainers의 `MariaDB`는 컨텍스트 단위로 초기화되며, 컨텍스트가 다르면 새 컨테이너가 생성됨
- 컨텍스트 B 생성 시, 기존 컨텍스트 A의 컨테이너는 종료되고 **새 포트에서 새로운 컨테이너가 실행됨**
- `UserSignupIntegrationTest(컨텍스트 A)` → `UserCleanupSchedulerIntegrationTest(컨텍스트 B)` 후에 `ValidationIntegrationTest` 실행 시 컨텍스트 A가 재사용되지만, 이미 연결되었던 MariaDB 컨테이너는 종료된 상태 → 연결 실패

**3️⃣ 이전 포트에 계속 연결을 시도하는 이유**

- `@DynamicPropertySource`는 컨텍스트 로딩 시점에 MariaDB의 포트를 고정하여 `spring.datasource.url`에 주입
- 컨텍스트가 캐싱되면 해당 JDBC URL도 그대로 유지되므로, **종료된 MariaDB 컨테이너의 포트로 계속 접속 시도함**

### 해결 방법

---

- **`@DirtiesContext`** 적용

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("integration-test")
    @Testcontainers
    @Transactional
    @Import(TestConfig.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

- `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)`의 역할
    - 테스트 클래스 실행이 끝난 뒤, Spring의 ApplicationContext 캐시를 제거하는 역할
    - 즉, 그 테스트 클래스 이후로는 같은 컨텍스트를 재사용하지 않도록 지정
    - 설정 별로 캐시를 폐기하는 시점이 다름
        - `AFTER_CLASS` : 해당 테스트 클래스 **끝난 후** 컨텍스트 캐시 **폐기**
        - `BEFORE_CLASS` : 해당 테스트 클래스 **시작 전에** 컨텍스트 캐시 **폐기**
    - 테스트 간에 상태 오염을 막고 싶거나 특정 테스트에서 다른 설정이나 DB 상태를 요구할 때 사용