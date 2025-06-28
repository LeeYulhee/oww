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
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public void test() {
        log.info("TEST = {}", properties.getVerificationTokenExpiry());
    }

    @Transactional
    public User create(CreateUserReq req) {
        User user = CreateUserReq.toEntity(req, passwordEncoder.encode(req.getPassword()));

        log.info("CREATED_AT = {}", user.getCreatedAt());

        return userRepository.save(user);
    }
}