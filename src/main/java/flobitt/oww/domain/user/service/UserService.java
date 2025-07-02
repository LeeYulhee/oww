package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.global.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AppProperties properties;

    public void test() {
        log.info("TEST = {}", properties.getVerificationTokenExpiry());
    }

    /**
     * User Entity 저장
     */
    public void createUser(User user) {
        userRepository.save(user);
    }

    /**
     * User 상태 ACTIVE로 변경
     */
    public void updateUserStatusActive(User user) {
        user.updateUserStatusActive();
    }

    /**
     * User 조회 : 이메일로, 삭제되지 않은 User
     */
    public User findByEmailAndIsDeletedFalse(String email) {
        // TODO Exception 설정
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("유효한 회원 정보가 없습니다."));
    }
}