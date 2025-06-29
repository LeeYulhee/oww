package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.repository.EmailVerificationRepository;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.global.properties.AppProperties;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public void test() {
        log.info("TEST = {}", properties.getVerificationTokenExpiry());
    }

    // user 생성
    @Transactional
    public void create(User user) {
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatusActive(User user) {
        user.updateUserStatusActive();
    }
}