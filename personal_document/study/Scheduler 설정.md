### 🔄 전체 실행 흐름
- 애플리케이션 시작 → YAML 로드
- Properties 빈 생성 → 설정값 주입
- 조건 확인 → @ConditionalOnProperty 검사
- 스케줄링 활성화 → @EnableScheduling 실행
- 스케줄러 등록 → @Scheduled 메서드 등록
- 실행 대기 → cron 표현식에 따라 대기
- 실행 시점 → ShedLock 확인 후 작업 실행

### ⚙️ 주요 설정들
- @EnableScheduling: 스프링 스케줄링 기능 활성화
- ThreadPoolTaskScheduler: 스케줄 전용 스레드 풀 설정
- ShedLock: 분산 환경에서 중복 실행 방지
- SpEL 표현식: 런타임에 Properties 값 참조

```
// 스케줄러 전체 흐름과 설정 상세 설명

// ============================================
// 1. 스프링 부트 애플리케이션 시작 과정
// ============================================

/*
애플리케이션 시작 순서:
1. application.yml 파일 로드
2. @ConfigurationProperties 클래스들 생성 및 값 주입
3. @Configuration 클래스들 실행
4. @Component, @Service 등 빈 생성
5. 스케줄러 등록 및 활성화
*/

// ============================================
// 2. @ConditionalOnProperty 상세 설명
// ============================================

@ConditionalOnProperty(
    name = "app.scheduling.enabled",     // 확인할 프로퍼티 이름
    havingValue = "true",                // 기대하는 값
    matchIfMissing = true                // 프로퍼티가 없을 때 기본 동작
)
public class SchedulingConfig {
    // 이 클래스는 언제 생성될까?
}

/*
@ConditionalOnProperty 동작 방식:

Case 1: app.scheduling.enabled=true (application.yml에 명시)
→ havingValue="true"와 일치 → 빈 생성 ✅

Case 2: app.scheduling.enabled=false (application.yml에 명시)  
→ havingValue="true"와 불일치 → 빈 생성 안함 ❌

Case 3: app.scheduling.enabled 프로퍼티가 아예 없음
→ matchIfMissing=true → 빈 생성 ✅ (기본값으로 활성화)

Case 4: matchIfMissing=false였다면?
→ 프로퍼티 없으면 → 빈 생성 안함 ❌
*/

// 실제 application.yml 예시들
/*
# Case 1: 명시적으로 활성화
app:
  scheduling:
    enabled: true  # ✅ 스케줄링 활성화

# Case 2: 명시적으로 비활성화  
app:
  scheduling:
    enabled: false # ❌ 스케줄링 비활성화

# Case 3: 프로퍼티 없음
app:
  other-config:
    value: something
# app.scheduling.enabled가 없음 → matchIfMissing=true → ✅ 활성화

# Case 4: 잘못된 값
app:
  scheduling:
    enabled: maybe # ❌ "true"가 아니므로 비활성화
*/

// ============================================
// 3. 전체 스케줄러 설정 흐름 단계별 설명
// ============================================

// Step 1: Properties 클래스 생성 및 값 주입
@Data
@Component
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {
    private boolean enabled = true;                    // 기본값
    private String userCleanupCron = "0 0 2 * * *";   // 기본값
    private long lockTimeout = 30;                     // 기본값
    private int maxRetries = 3;                        // 기본값
}

/*
스프링이 이렇게 동작:
1. application.yml에서 app.scheduler.* 찾기
2. 있으면 해당 값으로 설정, 없으면 기본값 사용
3. SchedulerProperties 빈 생성

application.yml 예시:
app:
  scheduler:
    enabled: true
    user-cleanup-cron: "0 30 1 * * *"  # 새벽 1시 30분으로 변경
    lock-timeout: 60
    max-retries: 5

결과:
- enabled = true (yml 값)
- userCleanupCron = "0 30 1 * * *" (yml 값)  
- lockTimeout = 60 (yml 값)
- maxRetries = 5 (yml 값)
*/

// Step 2: SchedulingConfig 실행 (조건부)
@Configuration
@EnableScheduling  // 🔥 이게 핵심! 스프링 스케줄링 기능 활성화
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄러 전용 스레드 풀 설정
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);                      // 동시 실행 가능한 스케줄 작업 수
        scheduler.setThreadNamePrefix("scheduler-");   // 스레드 이름 접두사
        scheduler.setAwaitTerminationSeconds(60);      // 종료 시 대기 시간
        scheduler.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 작업 완료 대기
        scheduler.initialize();
        
        taskRegistrar.setTaskScheduler(scheduler);
    }
}

/*
@EnableScheduling이 하는 일:
1. @Scheduled 어노테이션이 붙은 메서드들을 스캔
2. cron 표현식 파싱
3. 스케줄러에 작업 등록
4. 지정된 시간에 메서드 실행

ThreadPoolTaskScheduler 설정 의미:
- poolSize=3: 스케줄 작업 3개까지 동시 실행 가능
- threadNamePrefix: 로그에서 구분하기 쉽게 스레드 이름 설정
- awaitTerminationSeconds: 앱 종료 시 60초 대기
- waitForTasksToCompleteOnShutdown: 실행 중인 작업 완료까지 대기
*/

// Step 3: ShedLock 설정 (분산 환경 중복 실행 방지)
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "30m")  // 기본 락 시간: 30분
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // 🔥 중요! DB 시간 사용으로 서버 시간차 문제 방지
                .build()
        );
    }
}

/*
ShedLock이 필요한 이유:
- 운영 환경에서는 보통 여러 서버 인스턴스가 동시에 실행됨
- 스케줄러도 각 서버마다 실행됨
- 같은 작업이 여러 번 실행되면 문제 발생

ShedLock 동작 방식:
1. 스케줄 작업 실행 전에 DB에 락 생성
2. 다른 서버에서 같은 작업 시도 시 락 확인
3. 락이 있으면 실행 건너뛰기
4. 작업 완료 후 락 해제

shedlock 테이블 구조:
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,           -- 락 이름 (작업 구분)
    lock_until TIMESTAMP(3) NOT NULL,    -- 락 만료 시간
    locked_at TIMESTAMP(3) NOT NULL,     -- 락 획득 시간
    locked_by VARCHAR(255) NOT NULL,     -- 락 획득한 서버 정보
    PRIMARY KEY (name)
);
*/

// Step 4: 실제 스케줄러 클래스 생성
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class UnverifiedUserCleanupScheduler {

    private final UserService userService;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(cron = "#{@schedulerProperties.userCleanupCron}")  // 🔥 SpEL 표현식
    @SchedulerLock(
        name = "cleanupUnverifiedUsers",                          // 락 이름
        lockAtMostFor = "#{@schedulerProperties.lockTimeout}m",   // 최대 락 시간
        lockAtLeastFor = "1m"                                     // 최소 락 시간
    )
    public void cleanupUnverifiedUsers() {
        // 실제 작업 로직
    }
}

/*
@Scheduled 어노테이션 상세:
- cron = "#{@schedulerProperties.userCleanupCron}"
  → SpEL(Spring Expression Language) 사용
  → @schedulerProperties 빈의 userCleanupCron 필드 값 참조
  → 런타임에 Properties에서 실제 cron 값 가져옴

@SchedulerLock 어노테이션 상세:
- name: 락의 고유 이름 (여러 스케줄 작업 구분)
- lockAtMostFor: 최대 락 시간 (작업이 멈춰도 이 시간 후 락 해제)
- lockAtLeastFor: 최소 락 시간 (작업이 빨리 끝나도 이 시간까지는 락 유지)

왜 lockAtLeastFor가 필요할까?
- 작업이 너무 빨리 끝나면 다른 서버에서 거의 동시에 실행될 수 있음
- 1분은 안전한 간격 유지
*/

// ============================================
// 4. cron 표현식 상세 설명
// ============================================

/*
cron 표현식 구조: "초 분 시 일 월 요일"

"0 0 2 * * *" 분석:
- 0: 초 (0초)
- 0: 분 (0분)  
- 2: 시 (2시)
- *: 일 (모든 일)
- *: 월 (모든 월)
- *: 요일 (모든 요일)

결과: 매일 새벽 2시 0분 0초에 실행

다른 cron 예시들:
"0 30 1 * * *"     → 매일 새벽 1시 30분
"0 0 */6 * * *"    → 6시간마다 (0시, 6시, 12시, 18시)
"0 0 9 * * MON"    → 매주 월요일 오전 9시
"0 0 10 1 * *"     → 매월 1일 오전 10시
"0 0/30 * * * *"   → 30분마다
*/

// ============================================
// 5. 전체 실행 흐름 시퀀스
// ============================================

/*
1. 애플리케이션 시작
   ↓
2. application.yml 로드
   app.scheduling.enabled=true 확인
   ↓
3. SchedulerProperties 빈 생성
   - enabled=true, userCleanupCron="0 0 2 * * *" 등 설정
   ↓
4. @ConditionalOnProperty 조건 확인
   - app.scheduling.enabled=true → 조건 만족
   ↓
5. SchedulingConfig 빈 생성
   - @EnableScheduling 실행 → 스케줄링 기능 활성화
   - ThreadPoolTaskScheduler 설정
   ↓
6. ShedLockConfig 빈 생성  
   - 분산 락 기능 활성화
   ↓
7. UnverifiedUserCleanupScheduler 빈 생성
   - @Scheduled 메서드 등록
   ↓
8. 스케줄러 동작 시작
   - cron="0 0 2 * * *" → 매일 새벽 2시마다 실행 예약
   ↓
9. 실행 시점 (매일 새벽 2시)
   ↓
10. ShedLock 확인
    - DB에 "cleanupUnverifiedUsers" 락 있는지 확인
    - 없으면 락 생성하고 진행, 있으면 실행 건너뛰기
    ↓
11. 실제 작업 실행
    - cleanupUnverifiedUsers() 메서드 실행
    - UserService.deleteExpiredUnverifiedUsers() 호출
    ↓
12. 작업 완료 후 락 해제
*/

// ============================================
// 6. 환경별 설정 예시
// ============================================

// application.yml (공통 설정)
/*
app:
  scheduler:
    enabled: true
    user-cleanup-cron: "0 0 2 * * *"
    lock-timeout: 30
    max-retries: 3

---
# 로컬 개발 환경
spring:
  config:
    activate:
      on-profile: local
app:
  scheduler:
    enabled: false  # 로컬에서는 스케줄러 비활성화

---  
# 개발 서버
spring:
  config:
    activate:
      on-profile: dev
app:
  scheduler:
    enabled: true
    user-cleanup-cron: "0 */10 * * * *"  # 10분마다 (테스트용)

---
# 운영 서버
spring:
  config:
    activate:
      on-profile: prod
app:
  scheduler:
    enabled: true
    user-cleanup-cron: "0 0 2 * * *"     # 매일 새벽 2시
    lock-timeout: 60
    max-retries: 5
*/

// ============================================
// 7. 실행 중 로그 예시
// ============================================

/*
애플리케이션 시작 시:
2025-07-03 09:00:00 INFO  - SchedulingConfig Bean created
2025-07-03 09:00:00 INFO  - ShedLock initialized with JDBC provider
2025-07-03 09:00:00 INFO  - UnverifiedUserCleanupScheduler registered
2025-07-03 09:00:00 INFO  - Scheduled task 'cleanupUnverifiedUsers' with cron '0 0 2 * * *'

스케줄 실행 시 (새벽 2시):
2025-07-04 02:00:00 INFO  - === 미인증 사용자 정리 작업 시작: 2025-07-04 02:00:00 ===
2025-07-04 02:00:00 INFO  - ShedLock acquired: cleanupUnverifiedUsers
2025-07-04 02:00:00 INFO  - 미인증 사용자 정리 시도: 1/3
2025-07-04 02:00:01 INFO  - 미인증 사용자 정리 성공: 5명 처리 (시도: 1)
2025-07-04 02:00:01 INFO  - === 미인증 사용자 정리 작업 완료: 2025-07-04 02:00:01 | 처리 건수: 5명 ===
2025-07-04 02:00:01 INFO  - ShedLock released: cleanupUnverifiedUsers

다른 서버에서 동시 실행 시도:
2025-07-04 02:00:00 INFO  - ShedLock already acquired by another instance: cleanupUnverifiedUsers
2025-07-04 02:00:00 INFO  - Skipping scheduled execution
*/

// ============================================
// 8. 트러블슈팅 가이드
// ============================================

/*
Q1: 스케줄러가 실행되지 않아요!
A1: 확인 사항들
   - app.scheduling.enabled=true 인지 확인
   - 현재 프로파일에서 스케줄러가 활성화되어 있는지 확인
   - cron 표현식이 올바른지 확인
   - 로그에서 "Scheduled task registered" 메시지 확인

Q2: 여러 서버에서 중복 실행되어요!
A2: 확인 사항들  
   - ShedLock 설정이 되어 있는지 확인
   - shedlock 테이블이 생성되어 있는지 확인
   - 모든 서버가 같은 DB를 사용하는지 확인

Q3: 스케줄러가 멈춰요!
A3: 확인 사항들
   - lockAtMostFor 시간 확인 (너무 짧으면 작업 중에 락 해제)
   - DB 연결 상태 확인
   - ThreadPool 크기 확인 (동시 실행 작업이 많으면 대기)

Q4: 설정이 적용되지 않아요!
A4: 확인 사항들
   - @ConfigurationProperties 클래스에 @Component 있는지 확인
   - application.yml 들여쓰기가 올바른지 확인 (YAML은 들여쓰기에 민감)
   - SpEL 표현식이 올바른지 확인 (#{@beanName.property})
*/
```
### lockAtMostFor : 스케쥴링을 여러 서버에서 돌릴 때 필요(단일 서버에서는 사실 lockAtMostFor나 ShedLock 자체가 필요 없음)
**💥 문제 상황: lockAtMostFor가 없다면?**
```
02:00:00 - 서버A: 락 획득 → 작업 시작
02:03:00 - 서버A: 갑자기 장애! 💥 (락 해제 못함)
02:00:00 (다음날) - 서버B: 락 확인 → 아직 있음 → 실행 안함 ❌
02:00:00 (그 다음날) - 서버C: 락 확인 → 아직 있음 → 실행 안함 ❌
→ 영원히 실행되지 않음!
```

**✅ 해결책: lockAtMostFor 설정**
```java
@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "30m"  // 최대 30분 후에는 강제로 락 해제
)
```
```
02:00:00 - 서버A: 락 획득 (만료시간: 02:30:00)
02:03:00 - 서버A: 장애 발생! 💥
02:30:00 - 락 자동 만료 ⏰
03:00:00 - 서버B: 새로운 락 획득 → 정상 실행 ✅
```

**🎯 lockAtMostFor의 역할**
- 데드락 방지: 서버 장애 시 무한 대기 방지
- 자동 복구: 일정 시간 후 자동으로 락 해제
- 시스템 안정성: 한 서버 장애가 전체 스케줄링을 멈추지 않도록

**⚙️ 설정 가이드라인**
- 공식: lockAtMostFor = 예상 실행시간 × 3
- 예시: 10분 작업 → 30분 설정
- 이유: 너무 짧으면 중복실행, 너무 길면 복구지연

```
// lockAtMostFor가 필요한 이유

// ============================================
// 1. 문제 상황: lockAtMostFor가 없다면?
// ============================================

// 가상의 상황 (lockAtMostFor 없음)
@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(name = "cleanupUnverifiedUsers")  // lockAtMostFor 없음!
public void cleanupUnverifiedUsers() {
    // 5분 정도 걸리는 작업
    userService.deleteExpiredUnverifiedUsers();
}

/*
정상적인 실행:
2025-07-04 02:00:00 - 서버A: 락 획득 → 작업 시작
2025-07-04 02:05:00 - 서버A: 작업 완료 → 락 해제 ✅

문제가 생기는 경우:
2025-07-04 02:00:00 - 서버A: 락 획득 → 작업 시작
2025-07-04 02:03:00 - 서버A: 갑자기 장애 발생! 💥 (락 해제 못함)
2025-07-05 02:00:00 - 서버B: 락 확인 → 아직 락이 있음 → 실행 안함 ❌
2025-07-06 02:00:00 - 서버C: 락 확인 → 아직 락이 있음 → 실행 안함 ❌
2025-07-07 02:00:00 - ...영원히 실행 안됨! ❌❌❌

결과: 서버A 장애 이후로 스케줄러가 영원히 실행되지 않음!
*/

// ============================================
// 2. 해결책: lockAtMostFor 설정
// ============================================

@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "30m"  // 🔥 최대 30분 후에는 강제로 락 해제
)
public void cleanupUnverifiedUsers() {
    userService.deleteExpiredUnverifiedUsers();
}

/*
개선된 시나리오:
2025-07-04 02:00:00 - 서버A: 락 획득 → 작업 시작
                     DB에 lock_until = 2025-07-04 02:30:00 기록
2025-07-04 02:03:00 - 서버A: 갑자기 장애 발생! 💥
2025-07-04 02:30:00 - 시간이 지나면서 락이 자동 만료됨 ⏰
2025-07-05 02:00:00 - 서버B: 락 확인 → 만료된 락 발견 → 새로운 락 획득 → 실행 ✅

결과: 서버A 장애가 있어도 다음날 정상 실행됨!
*/

// ============================================
// 3. 실제 DB 테이블에서 보는 동작
// ============================================

// shedlock 테이블 구조
/*
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,           -- 락 이름
    lock_until TIMESTAMP(3) NOT NULL,    -- 🔥 이 시간까지만 락 유효
    locked_at TIMESTAMP(3) NOT NULL,     -- 락 획득 시간
    locked_by VARCHAR(255) NOT NULL,     -- 락 획득한 서버
    PRIMARY KEY (name)
);
*/

// 정상 실행 시 DB 상태 변화
/*
실행 전:
shedlock 테이블 비어있음

2025-07-04 02:00:00 - 스케줄러 실행 시작:
INSERT INTO shedlock VALUES (
    'cleanupUnverifiedUsers',
    '2025-07-04 02:30:00',  -- 현재시간 + 30분 (lockAtMostFor)
    '2025-07-04 02:00:00',  -- 현재시간
    'server-A-12345'        -- 서버 식별자
);

2025-07-04 02:05:00 - 작업 완료 후:
DELETE FROM shedlock WHERE name = 'cleanupUnverifiedUsers';
*/

// 서버 장애 시 DB 상태
/*
2025-07-04 02:00:00 - 서버A 스케줄러 시작:
shedlock 테이블:
┌──────────────────────┬─────────────────────┬─────────────────────┬───────────────┐
│ name                 │ lock_until          │ locked_at           │ locked_by     │
├──────────────────────┼─────────────────────┼─────────────────────┼───────────────┤
│ cleanupUnverifiedUsers│ 2025-07-04 02:30:00│ 2025-07-04 02:00:00│ server-A-12345│
└──────────────────────┴─────────────────────┴─────────────────────┴───────────────┘

2025-07-04 02:03:00 - 서버A 장애 발생
(DB 상태 그대로 유지됨)

2025-07-04 02:30:01 - 락 만료 후 서버B 실행 시도:
ShedLock 확인 로직:
SELECT * FROM shedlock 
WHERE name = 'cleanupUnverifiedUsers' 
AND lock_until > NOW();  -- 현재 시간보다 lock_until이 큰 것만

결과: 레코드 없음 (락 만료됨) → 새로운 락 생성 가능!
*/

// ============================================
// 4. lockAtMostFor 시간 설정 가이드
// ============================================

// ❌ 너무 짧게 설정한 경우
@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "5m"  // 작업이 10분 걸리는데 5분으로 설정
)
public void cleanupUnverifiedUsers() {
    // 10분 걸리는 작업
    userService.deleteExpiredUnverifiedUsers();
}

/*
문제 상황:
02:00:00 - 서버A: 락 획득 → 작업 시작
02:05:00 - 락 만료 (작업은 아직 진행 중)
02:05:01 - 서버B: 락 없음 확인 → 새로운 락 획득 → 작업 시작
결과: 같은 작업이 동시에 2번 실행됨! ❌❌
*/

// ✅ 적절하게 설정한 경우
@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "30m"  // 작업이 10분 걸리는데 넉넉하게 30분
)
public void cleanupUnverifiedUsers() {
    // 10분 걸리는 작업
    userService.deleteExpiredUnverifiedUsers();
}

/*
정상 상황:
02:00:00 - 서버A: 락 획득 → 작업 시작  
02:10:00 - 서버A: 작업 완료 → 락 해제
결과: 정상 실행 ✅

장애 상황:
02:00:00 - 서버A: 락 획득 → 작업 시작
02:05:00 - 서버A: 장애 발생
02:30:00 - 락 만료
03:00:00 - 서버B: 다음 실행 시간에 정상 실행
결과: 장애 복구 후 정상 실행 ✅
*/

// ============================================
// 5. 실제 설정 예시와 권장 사항
// ============================================

// 작업별 권장 lockAtMostFor 시간
public class SchedulerExamples {
    
    // 1. 빠른 작업 (1-2분)
    @SchedulerLock(lockAtMostFor = "10m")
    public void quickCleanup() {
        // 임시 파일 정리 등
    }
    
    // 2. 중간 작업 (5-10분)  
    @SchedulerLock(lockAtMostFor = "30m")
    public void mediumTask() {
        // 사용자 정리, 이메일 발송 등
    }
    
    // 3. 긴 작업 (30분-1시간)
    @SchedulerLock(lockAtMostFor = "2h")
    public void longTask() {
        // 대용량 데이터 처리, 리포트 생성 등
    }
    
    // 4. 매우 긴 작업 (수 시간)
    @SchedulerLock(lockAtMostFor = "6h")
    public void veryLongTask() {
        // 전체 데이터 마이그레이션 등
    }
}

// 권장 공식: lockAtMostFor = 예상 실행 시간 × 3
/*
이유:
- 예상 시간의 3배 정도면 대부분의 지연 상황 커버 가능
- 너무 길면 장애 시 복구가 늦어짐
- 너무 짧으면 정상 실행 중에 락이 풀릴 위험

예시:
- 예상 실행 시간: 10분
- 권장 lockAtMostFor: 30분
- 최대 허용: 60분 (그 이상은 너무 김)
*/

// ============================================
// 6. lockAtLeastFor와의 차이점
// ============================================

@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "30m",   // 최대 30분 후 강제 해제
    lockAtLeastFor = "1m"    // 최소 1분간은 락 유지
)
public void cleanupUnverifiedUsers() {
    userService.deleteExpiredUnverifiedUsers();
}

/*
lockAtMostFor: 
- 목적: 데드락 방지 (서버 장애 시 무한 대기 방지)
- 의미: "아무리 늦어도 이 시간 후에는 락 해제"

lockAtLeastFor:
- 목적: 너무 빠른 중복 실행 방지  
- 의미: "아무리 빨리 끝나도 이 시간까지는 락 유지"

실제 시나리오:
02:00:00 - 락 획득 (lock_until = 02:30:00)
02:00:30 - 작업 완료 (30초만에 끝남)
02:01:00 - lockAtLeastFor 시간 경과 → 락 해제 가능
결과: 빠르게 끝나도 1분간은 다른 서버에서 실행 안됨
*/

// ============================================
// 7. 설정값을 Properties로 관리하는 이유
// ============================================

// application.yml
/*
app:
  scheduler:
    lock-timeout: 30  # 분 단위

# 환경별 다른 설정 가능
---
spring:
  config:
    activate:
      on-profile: dev
app:
  scheduler:
    lock-timeout: 10  # 개발환경에서는 짧게

---  
spring:
  config:
    activate:
      on-profile: prod
app:
  scheduler:
    lock-timeout: 60  # 운영환경에서는 길게
*/

// 코드에서 사용
@SchedulerLock(
    name = "cleanupUnverifiedUsers",
    lockAtMostFor = "#{@schedulerProperties.lockTimeout}m"  // Properties에서 동적으로 가져옴
)
public void cleanupUnverifiedUsers() {
    // ...
}

/*
장점:
1. 환경별로 다른 설정 가능
2. 재배포 없이 설정 변경 가능 (Config Server 사용 시)
3. 운영 중 튜닝 가능
4. 테스트에서 짧은 시간으로 설정 가능
*/

// ============================================
// 8. 실제 장애 상황과 복구 시나리오
// ============================================

/*
시나리오 1: 서버 재시작
02:00:00 - 서버A: 락 획득 → 작업 시작
02:05:00 - 서버A: 재시작으로 인한 프로세스 종료
02:05:01 - 서버A: 재시작 완료
02:30:00 - 락 만료
03:00:00 - 다음 스케줄 시간에 정상 실행

시나리오 2: 네트워크 단절
02:00:00 - 서버A: 락 획득 → 작업 시작  
02:10:00 - 서버A: DB 연결 끊어짐 → 작업 실패
02:30:00 - 락 만료 (서버A가 락 해제 못했지만 자동 만료)
03:00:00 - 서버B: 정상 실행

시나리오 3: 무한 루프
02:00:00 - 서버A: 락 획득 → 버그로 인한 무한 루프
02:30:00 - 락 만료 (무한 루프 중이지만 강제 만료)
03:00:00 - 서버B: 새로운 락 획득 → 정상 실행 (버그 수정 후)
*/

// 결론
/*
lockAtMostFor는:
✅ 서버 장애 시 무한 대기 방지
✅ 데드락 상황 자동 해결  
✅ 시스템 안정성 향상
✅ 운영 중 장애 자동 복구

설정 가이드라인:
- 예상 실행 시간의 2-3배로 설정
- 너무 짧으면 중복 실행 위험
- 너무 길면 장애 복구가 늦어짐
- 환경별로 다르게 설정 권장
*/
```
```
// 단일 서버 vs 다중 서버 스케줄링

// ============================================
// 1. 단일 서버 환경 (ShedLock 불필요)
// ============================================

// 단일 서버에서는 이렇게만 해도 충분
@Component
public class SimpleScheduler {
    
    @Scheduled(cron = "0 0 2 * * *")  // ShedLock 없어도 됨!
    public void cleanupUsers() {
        log.info("미인증 사용자 정리 시작");
        userService.deleteExpiredUnverifiedUsers();
        log.info("미인증 사용자 정리 완료");
    }
}

/*
단일 서버 상황:
┌─────────────┐
│   서버 A    │ ← 스케줄러 실행
│             │
└─────────────┘

장점:
✅ 설정 간단 (ShedLock 설정 불필요)
✅ 의존성 적음 (DB 락 테이블 불필요)
✅ 성능 오버헤드 없음

단점:
❌ 서버 장애 시 백업 없음
❌ 확장성 제한
*/

// ============================================
// 2. 다중 서버 환경 (ShedLock 필수)
// ============================================

// 다중 서버에서는 ShedLock 필수!
@Component
public class DistributedScheduler {
    
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
        name = "cleanupUsers",
        lockAtMostFor = "30m",
        lockAtLeastFor = "1m"
    )
    public void cleanupUsers() {
        log.info("미인증 사용자 정리 시작");
        userService.deleteExpiredUnverifiedUsers();
        log.info("미인증 사용자 정리 완료");
    }
}

/*
다중 서버 상황:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   서버 A    │    │   서버 B    │    │   서버 C    │
│  스케줄러   │    │  스케줄러   │    │  스케줄러   │
└─────────────┘    └─────────────┘    └─────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                    ┌───────▼───────┐
                    │   공유 DB     │ ← ShedLock 테이블
                    │  (락 관리)    │
                    └───────────────┘

장점:
✅ 고가용성 (한 서버 장애 시 다른 서버에서 실행)
✅ 확장성 (서버 추가 가능)
✅ 중복 실행 방지

단점:
❌ 설정 복잡 (ShedLock 설정 필요)
❌ 의존성 증가 (DB 락 테이블 필요)
❌ 약간의 성능 오버헤드
*/

// ============================================
// 3. ShedLock 없이 다중 서버 실행하면?
// ============================================

// ShedLock 없는 다중 서버 (문제 상황)
@Component
public class ProblematicScheduler {
    
    @Scheduled(cron = "0 0 2 * * *")  // ShedLock 없음!
    public void cleanupUsers() {
        userService.deleteExpiredUnverifiedUsers();
    }
}

/*
문제 상황:
2025-07-04 02:00:00 정각

서버 A: "스케줄 시간이야! 사용자 정리 시작!"
        ↓ 데이터 조회: 만료된 사용자 100명 발견
        ↓ 삭제 시작...

서버 B: "스케줄 시간이야! 사용자 정리 시작!" (동시에!)
        ↓ 데이터 조회: 만료된 사용자 100명 발견 (같은 데이터!)
        ↓ 삭제 시작...

서버 C: "스케줄 시간이야! 사용자 정리 시작!" (동시에!)
        ↓ 데이터 조회: 만료된 사용자 100명 발견 (같은 데이터!)
        ↓ 삭제 시작...

결과:
❌ 같은 작업이 3번 동시 실행
❌ DB 부하 3배 증가
❌ 불필요한 리소스 낭비
❌ 로그 중복으로 혼란
*/

// ============================================
// 4. ShedLock으로 해결된 상황
// ============================================

/*
ShedLock 적용 후:
2025-07-04 02:00:00 정각

서버 A: "스케줄 시간이야!"
        ↓ DB 락 시도: "cleanupUsers" 락 생성 성공!
        ↓ 사용자 정리 작업 실행 ✅

서버 B: "스케줄 시간이야!"
        ↓ DB 락 시도: "cleanupUsers" 락 이미 있음
        ↓ "다른 서버에서 실행 중이구나. 스킵!" ✅

서버 C: "스케줄 시간이야!"
        ↓ DB 락 시도: "cleanupUsers" 락 이미 있음  
        ↓ "다른 서버에서 실행 중이구나. 스킵!" ✅

결과:
✅ 한 서버에서만 실행
✅ 리소스 효율적 사용
✅ 중복 작업 방지
*/

// ============================================
// 5. 실제 운영 환경별 선택 가이드
// ============================================

// 케이스 1: 소규모 서비스 (단일 서버)
/*
상황: 
- 서버 1대로 충분
- 고가용성 요구사항 낮음
- 관리 복잡성 최소화 원함

추천 설정:
*/
@Component
public class SimpleScheduler {
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupUsers() {
        userService.deleteExpiredUnverifiedUsers();
    }
}
// ShedLock 설정 없음, 단순하게 운영

// 케이스 2: 중간 규모 서비스 (다중 서버)
/*
상황:
- 서버 2-3대 운영
- 고가용성 필요
- 로드밸런싱 적용

추천 설정:
*/
@Component
public class DistributedScheduler {
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "cleanupUsers", lockAtMostFor = "30m")
    public void cleanupUsers() {
        userService.deleteExpiredUnverifiedUsers();
    }
}
// ShedLock 적용, 안정적 운영

// 케이스 3: 대규모 서비스 (MSA)
/*
상황:
- 마이크로서비스 아키텍처
- 서버 수십 대
- 높은 안정성 요구

추천 설정:
*/
@Component
public class EnterpriseScheduler {
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
        name = "cleanupUsers",
        lockAtMostFor = "#{@schedulerProperties.lockTimeout}m",
        lockAtLeastFor = "1m"
    )
    public void cleanupUsers() {
        try {
            userService.deleteExpiredUnverifiedUsers();
        } catch (Exception e) {
            alertService.sendAlert("스케줄러 실패", e);
            throw e;
        }
    }
}
// ShedLock + 모니터링 + 알림 + 설정 외부화

// ============================================
// 6. 환경 감지해서 자동으로 ShedLock 적용/미적용
// ============================================

// 환경별 자동 설정
@Configuration
public class SchedulerAutoConfig {
    
    @Value("${app.deployment.mode:single}")  // single or cluster
    private String deploymentMode;
    
    @Bean
    @ConditionalOnProperty(name = "app.deployment.mode", havingValue = "cluster")
    public LockProvider lockProvider(DataSource dataSource) {
        // 클러스터 모드일 때만 ShedLock 활성화
        return new JdbcTemplateLockProvider(dataSource);
    }
}

// application.yml
/*
# 단일 서버 환경
app:
  deployment:
    mode: single  # ShedLock 비활성화

# 클러스터 환경  
app:
  deployment:
    mode: cluster # ShedLock 활성화
*/

// ============================================
// 7. 실제 운영 시나리오
// ============================================

/*
시나리오 A: 스타트업 (단일 서버)
- 서버: AWS EC2 t3.medium 1대
- 설정: ShedLock 없음
- 이유: 단순함 > 고가용성

시나리오 B: 성장하는 서비스 (2-3대 서버)  
- 서버: AWS EC2 c5.large 3대 (로드밸런서)
- 설정: ShedLock 적용
- 이유: 중복 실행 방지 필요

시나리오 C: 대기업 서비스 (수십 대 서버)
- 서버: Kubernetes 클러스터 (Auto Scaling)
- 설정: ShedLock + 모니터링 + 알림
- 이유: 안정성과 확장성 모두 중요

시나리오 D: 개발/테스트 환경
- 서버: 개발자 로컬 + 개발 서버
- 설정: 환경별 다른 설정
- 이유: 개발 시 스케줄러 간섭 방지
*/

// ============================================
// 8. 정리 및 선택 가이드
// ============================================

/*
ShedLock이 필요한 경우:
✅ 여러 서버에서 같은 애플리케이션 실행
✅ 로드밸런서 뒤에 여러 인스턴스
✅ Docker Swarm, Kubernetes 등 오케스트레이션 사용
✅ Auto Scaling으로 서버 개수 가변적
✅ 고가용성이 중요한 서비스

ShedLock이 불필요한 경우:
✅ 단일 서버에서만 실행
✅ 개발자 로컬 환경
✅ 프로토타입이나 간단한 서비스
✅ 스케줄러 중복 실행이 문제없는 경우 (읽기 전용 작업 등)

결론:
단일 서버 → ShedLock 불필요 (간단하게)
다중 서버 → ShedLock 필수 (안정적으로)
*/
```
**단일 서버: ShedLock 불필요**
```java
@Scheduled(cron = "0 0 2 * * *")  // 이것만으로 충분!
public void cleanupUsers() { ... }
```
**다중 서버: ShedLock 필수**
```java
@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(name = "cleanup", lockAtMostFor = "30m")  // 중복 방지!
public void cleanupUsers() { ... }
```
**🌐 다중 서버 환경**
* 로드밸런서 뒤에 여러 서버
* Kubernetes 클러스터
* Auto Scaling 그룹
* Docker Swarm 환경

**⚡ ShedLock 없으면 문제**
```
02:00:00 - 서버A: 사용자 100명 삭제 시작
02:00:00 - 서버B: 사용자 100명 삭제 시작 (같은 작업!)
02:00:00 - 서버C: 사용자 100명 삭제 시작 (같은 작업!)
→ 3배 부하, 리소스 낭비!
```

**🛡️ ShedLock 있으면 해결**
```
02:00:00 - 서버A: 락 획득 → 작업 실행
02:00:00 - 서버B: 락 확인 → "이미 실행 중" → 스킵
02:00:00 - 서버C: 락 확인 → "이미 실행 중" → 스킵
→ 한 서버에서만 실행!
```
**결론: 여러 서버 = ShedLock 필수, 단일 서버 = ShedLock 불필요 입니다!**

### Thread.sleep(5000 * attempt); // 점진적 대기(Progressive Backoff) 전략