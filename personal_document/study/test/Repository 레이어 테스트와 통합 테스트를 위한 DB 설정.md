### 원하는 방향
- Repository 테스트는 H2 DB 설정 : JPA 기능만 테스트하기 위해
- Integration 테스트는 MariaDB 설정 : 실제 DB에서 잘 작동하는지도 확인하기 위해

### 최초 설정 방식
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
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
```yaml
# 통합 테스트용
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
```

# 이렇게 하고 Repository 테스트를 진행하니 에러 발생


## 1. H2 데이터베이스 시작 로그

```

2025-07-07T22:27:46.779+09:00  INFO 17792 --- [    Test worker] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:cc90a30c-bcab-4e1c-99db-c38d4a41447d;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'

```

- **`jdbc:h2:mem:`** - H2 인메모리 데이터베이스 URL
- **`EmbeddedDatabaseFactory`** - 임베디드 데이터베이스 팩토리가 H2를 시작

## 2. 데이터소스 교체 로그

```

2025-07-07T22:27:46.658+09:00  INFO 17792 --- [    Test worker] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version

```

- 운영용 데이터소스를 **임베디드 버전(H2)으로 교체**했다는 메시지

## 3. P6Spy 로그의 H2 URL

```

2025-07-07T22:27:47.017+09:00  INFO 17792 --- [    Test worker] p6spy : #1751894867017 | took 0ms | statement | connection 1| url jdbc:h2:mem:cc90a30c-bcab-4e1c-99db-c38d4a41447d

```

- SQL 로깅 도구인 P6Spy가 **`jdbc:h2:mem:`** URL을 표시

## 4. 실제 SQL 오류 발생 위치

```

Failed to execute SQL script statement #9 of file [C:\Users\reaso\IdeaProjects\oww\build\resources\main\data.sql]: CREATE TABLE `USERS` (...)

```

- MySQL 문법으로 작성된 `data.sql` 파일을 H2에서 실행하려다가 실패

이 로그들을 통해 **테스트 환경에서 H2 인메모리 데이터베이스가 자동으로 시작되어 MySQL 문법의 SQL 스크립트를 실행하려다가 호환성 문제로 실패**했음을 확인할 수 있습니다.

# 해결 방법
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: never  # data.sql 실행 비활성화
  jpa:
    hibernate:
      ddl-auto: create-drop  # JPA 엔티티로 테이블 생성
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
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