**🔵 트랜잭션이란?**
- 데이터베이스 작업의 논리적 단위
- ACID 원칙: 원자성, 일관성, 격리성, 지속성
- 목적: 데이터 일관성과 무결성 보장

**🔵 MyBatis에서도 트랜잭션 사용**
- JPA와 완전히 동일: @Transactional 어노테이션 사용
- Spring이 관리: Connection 획득/반환, commit/rollback 자동 처리
- 차이점: SQL 작성 방식만 다름 (JPA는 자동, MyBatis는 수동)

**🔵 실무에서 중요한 점**
```java
// MyBatis든 JPA든 이런 식으로 사용
@Service
@Transactional(readOnly = true)
public class UserService {
    
    public User findUser() { /* 조회 */ }
    
    @Transactional
    public void createUser() { /* 생성 */ }
}
```

```java
// 1. 트랜잭션이란?

/*
트랜잭션(Transaction) = 데이터베이스 작업의 논리적 단위

ACID 원칙:
- Atomicity (원자성): 모든 작업이 성공하거나 모두 실패
- Consistency (일관성): 데이터의 무결성 유지  
- Isolation (격리성): 동시 실행되는 트랜잭션들이 서로 영향 안줌
- Durability (지속성): 커밋된 데이터는 영구적으로 저장
*/

// 트랜잭션이 없다면?
public void transferMoney(String fromAccount, String toAccount, int amount) {
    // 1. A 계좌에서 돈 빼기
    accountRepository.updateBalance(fromAccount, -amount); // 성공
    
    // 2. 여기서 서버 장애 발생! 💥
    
    // 3. B 계좌에 돈 넣기 (실행 안됨)
    accountRepository.updateBalance(toAccount, +amount);   // 실행 안됨
    
    // 결과: A 계좌에서만 돈이 사라짐! 😱
}

// 트랜잭션이 있다면?
@Transactional
public void transferMoneyWithTransaction(String fromAccount, String toAccount, int amount) {
    // === 트랜잭션 시작 ===
    
    // 1. A 계좌에서 돈 빼기
    accountRepository.updateBalance(fromAccount, -amount); // 임시 저장
    
    // 2. 여기서 서버 장애 발생! 💥
    
    // 3. B 계좌에 돈 넣기 (실행 안됨)
    accountRepository.updateBalance(toAccount, +amount);   // 실행 안됨
    
    // === 트랜잭션 자동 롤백 ===
    // 결과: 모든 변경사항이 취소됨. A 계좌 원상복구! ✅
}

// 2. 데이터베이스 레벨에서의 트랜잭션

// SQL로 보는 트랜잭션
/*
-- 트랜잭션 시작
BEGIN; 

-- 작업 1
UPDATE accounts SET balance = balance - 100 WHERE account_id = 'A';

-- 작업 2  
UPDATE accounts SET balance = balance + 100 WHERE account_id = 'B';

-- 모든 작업 확정 (성공 시)
COMMIT;

-- 또는 모든 작업 취소 (실패 시)
ROLLBACK;
*/

// 3. JPA vs MyBatis에서 트랜잭션 사용

// === JPA 방식 ===
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA가 자동으로 트랜잭션 관리
}

@Service
@Transactional(readOnly = true)
public class UserService {
    
    @Transactional
    public void createUser(User user) {
        userRepository.save(user); // JPA가 트랜잭션 내에서 실행
    }
}

// === MyBatis 방식 ===
@Mapper
public interface UserMapper {
    void insertUser(User user);
    User selectUser(Long id);
    void updateUser(User user);
    void deleteUser(Long id);
}

@Service
@Transactional(readOnly = true) // MyBatis도 똑같이 @Transactional 사용!
public class UserService {
    
    private final UserMapper userMapper;
    
    @Transactional
    public void createUser(User user) {
        userMapper.insertUser(user); // MyBatis가 트랜잭션 내에서 실행
    }
    
    public User getUser(Long id) {
        return userMapper.selectUser(id); // readOnly 트랜잭션에서 실행
    }
}

// 4. MyBatis XML에서의 트랜잭션

<!-- UserMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.UserMapper">
    
    <!-- 단순 조회 쿼리 -->
    <select id="selectUser" resultType="User">
        SELECT user_id, email, user_status 
        FROM users 
        WHERE user_id = #{userId}
    </select>
    
    <!-- 쓰기 쿼리 -->
    <insert id="insertUser" parameterType="User">
        INSERT INTO users (user_id, email, password, user_status, created_at)
        VALUES (#{userId}, #{email}, #{password}, #{userStatus}, NOW())
    </insert>
    
    <!-- 복잡한 업데이트 쿼리 -->
    <update id="updateUserStatus">
        UPDATE users 
        SET user_status = #{status}, 
            email_verified_at = NOW()
        WHERE user_id = #{userId}
    </update>
    
</mapper>

// 5. MyBatis에서 트랜잭션 동작 원리

@Service
public class UserService {
    
    private final UserMapper userMapper;
    
    // 트랜잭션 없는 경우
    public void createUserWithoutTransaction(User user) {
        // 매번 새로운 DB 커넥션 사용
        userMapper.insertUser(user);        // 커넥션 1 사용 → 즉시 커밋 → 반환
        userMapper.insertProfile(profile);  // 커넥션 2 사용 → 즉시 커밋 → 반환
        
        // 문제: insertProfile 실패해도 insertUser는 이미 커밋됨
    }
    
    // 트랜잭션 있는 경우
    @Transactional
    public void createUserWithTransaction(User user) {
        // 같은 DB 커넥션을 계속 사용
        userMapper.insertUser(user);        // 커넥션 1 사용 → 임시 저장
        userMapper.insertProfile(profile);  // 커넥션 1 사용 → 임시 저장
        
        // 메서드 종료 시: 모든 작업 함께 커밋 또는 롤백
    }
}

// 6. 실제 MyBatis 설정 예시

// application.yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/oww
    username: root
    password: password
    
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

// MyBatis Configuration
@Configuration
@MapperScan("com.example.mapper") // MyBatis 매퍼 스캔
public class MyBatisConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
        // MyBatis도 Spring의 트랜잭션 매니저 사용!
    }
}

// 7. MyBatis vs JPA 트랜잭션 비교

// MyBatis 서비스
@Service
@Transactional(readOnly = true)
public class MyBatisUserService {
    
    private final UserMapper userMapper;
    
    // 조회
    public User findUser(Long id) {
        return userMapper.selectUser(id);
        // MyBatis가 readOnly 트랜잭션에서 쿼리 실행
    }
    
    // 생성
    @Transactional
    public void createUser(User user) {
        userMapper.insertUser(user);
        // MyBatis가 쓰기 트랜잭션에서 쿼리 실행
    }
    
    // 복잡한 업데이트
    @Transactional
    public void updateUserWithProfile(Long userId, User user, Profile profile) {
        userMapper.updateUser(user);           // 쿼리 1
        profileMapper.updateProfile(profile);  // 쿼리 2
        
        // 둘 다 성공하면 커밋, 하나라도 실패하면 롤백
    }
}

// JPA 서비스 (비교용)
@Service
@Transactional(readOnly = true)
public class JpaUserService {
    
    private final UserRepository userRepository;
    
    // 조회
    public User findUser(Long id) {
        return userRepository.findById(id).orElseThrow();
        // JPA가 readOnly 트랜잭션에서 쿼리 실행
    }
    
    // 생성  
    @Transactional
    public void createUser(User user) {
        userRepository.save(user);
        // JPA가 쓰기 트랜잭션에서 쿼리 실행
    }
}

// 8. MyBatis에서 수동 트랜잭션 관리 (옛날 방식, 비추천)

@Service
public class OldStyleUserService {
    
    private final SqlSession sqlSession;
    
    public void createUserManually(User user) {
        try {
            // 수동으로 트랜잭션 시작
            sqlSession.getConnection().setAutoCommit(false);
            
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            mapper.insertUser(user);
            
            // 수동 커밋
            sqlSession.commit();
            
        } catch (Exception e) {
            // 수동 롤백
            sqlSession.rollback();
            throw e;
        }
        
        // 👎 복잡하고 실수하기 쉬움!
        // Spring의 @Transactional 사용하는 게 훨씬 좋음
    }
}

// 9. 결론: MyBatis에서도 트랜잭션은 똑같이 중요!

/*
JPA든 MyBatis든:
1. @Transactional 어노테이션 사용법 동일
2. 트랜잭션 경계 설정 방식 동일  
3. Spring이 트랜잭션 관리 (Connection, commit/rollback)
4. 개발자는 비즈니스 로직에만 집중

차이점:
- JPA: 엔티티 중심, 자동 쿼리 생성
- MyBatis: SQL 중심, 수동 쿼리 작성

하지만 트랜잭션 관리는 완전히 동일!
*/
```