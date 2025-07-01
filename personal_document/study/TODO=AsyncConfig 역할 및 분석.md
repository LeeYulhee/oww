### Application main 메서드보다 Config 파일을 만드는 게 좋은 이유

### TODO : Config 파일 분석

1. 관심사 분리 (Separation of Concerns)
```java
// ❌ 메인 클래스에 모든 설정
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableCaching
public class OwwApplication {
    // 메인 클래스가 너무 많은 책임을 가짐
}

// ✅ 기능별로 분리
@SpringBootApplication  // 애플리케이션 시작만 담당
public class OwwApplication { /* ... */ }

@Configuration
@EnableAsync  // 비동기 설정만 담당
public class AsyncConfig { /* ... */ }

@Configuration
@EnableScheduling  // 스케줄링 설정만 담당
public class SchedulingConfig { /* ... */ }
```
2. 설정 관리 용이성
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    // 비동기 관련 모든 설정이 한 곳에
    @Bean("emailExecutor")
    public Executor emailExecutor() { /* 이메일 전용 */ }
    
    @Bean("fileUploadExecutor") 
    public Executor fileUploadExecutor() { /* 파일 업로드 전용 */ }
    
    @Bean("notificationExecutor")
    public Executor notificationExecutor() { /* 알림 전용 */ }
}
```
3. 조건부 활성화 쉬움
```java
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig {
    // 환경별로 비동기 기능 켜고 끄기 가능
}
```
4. 테스트 격리
```java
@TestConfiguration
public class TestAsyncConfig {
    
    @Bean
    @Primary
    public Executor taskExecutor() {
        // 테스트용 동기 실행기
        return new SyncTaskExecutor();
    }
}
```

<br>

**🏢 실무에서 많이 사용하는 패턴**

   대부분의 Spring Boot 프로젝트 구조:
```
   config/
   ├── AsyncConfig.java          @EnableAsync
   ├── SecurityConfig.java       @EnableWebSecurity  
   ├── JpaConfig.java            @EnableJpaAuditing
   ├── RedisConfig.java          @EnableRedisRepositories
   ├── SchedulingConfig.java     @EnableScheduling
   └── SwaggerConfig.java        @EnableSwagger2

OwwApplication.java           @SpringBootApplication (깔끔)
```