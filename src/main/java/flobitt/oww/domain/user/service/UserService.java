package flobitt.oww.domain.user.service;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.domain.user.repository.UserRepository;
import flobitt.oww.global.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public void test() {
        log.info("TEST = {}", appProperties.getVerificationTokenExpiry());
    }

    /**
     * User Entity 저장
     */
    @Transactional
    public void createUser(User user) {
        userRepository.save(user);
    }

    /**
     * User 상태 ACTIVE로 변경
     */
    @Transactional
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

    /**
     * 24시간이 지난 미인증 사용자 삭제 처리(배치 작업)
     */
    @Transactional
    public int deleteExpiredUnverifiedUsers() {
        LocalDateTime cutoffTime = calculateCutoffTime(appProperties.getVerificationTokenExpiry(), false);
        List<User> expiredUsers = findExpiredUnverifiedUsers(cutoffTime);

        if (expiredUsers.isEmpty()) {
            log.info("삭제할 미인증 사용자가 없습니다");
            return 0;
        }

        expiredUsers.forEach(this::deleteUser);
        return expiredUsers.size();
    }

    /**
     * 7일이 지난 삭제된 사용자 완전 삭제 처리(배치 작업)
     */
    @Transactional
    public int hardDeleteExpiredDeletedUsers() {
        LocalDateTime cutoffTime = calculateCutoffTime(appProperties.getHardDeleteDays(), true);
        log.info("@@@@@@@@@@마감시간 : = {}", cutoffTime);
        List<User> expiredDeletedUsers = findExpiredDeletedUsers(cutoffTime);

        if (expiredDeletedUsers.isEmpty()) {
            log.info("완전 삭제할 사용자가 없습니다");
            return 0;
        }

        expiredDeletedUsers.forEach(this::hardDeleteUser);
        return expiredDeletedUsers.size();
    }

    /**
     * 마감 시간(cutoffTime) 계산
     */
    private LocalDateTime calculateCutoffTime(int period, boolean isDays) {
        LocalDateTime now = LocalDateTime.now();
        return isDays ? now.minusDays(period) : now.minusHours(period);
    }

    /**
     * User 조회 : 사용자 상태 NOT_VERIFIED, 사용자 생성 시간이 24시간 넘은 사용자 목록
     */
    private List<User> findExpiredUnverifiedUsers(LocalDateTime cutoffTime) {
        return userRepository.findExpiredUnverifiedUsers(UserStatus.NOT_VERIFIED, cutoffTime);
    }

    /**
     * User 조회 : 삭제된 사용자 중 7일이 지난 사용자 목록
     */
    private List<User> findExpiredDeletedUsers(LocalDateTime cutoffTime) {
        return userRepository.findExpiredDeletedUsers(cutoffTime);
    }

    /**
     * User 삭제 : Soft Delete
     */
    @Transactional
    private void deleteUser(User user) {
        user.delete();
        log.debug("미인증 사용자 삭제 처리: userId={}, email={}",
                user.getUserLoginId(), user.getEmail());
    }

    /**
     * User 완전 삭제 : Hard Delete
     */
    @Transactional
    private void hardDeleteUser(User user) {
        userRepository.delete(user);
        log.debug("사용자 완전 삭제 처리: userId={}, email={}, deletedAt={}",
                user.getUserLoginId(), user.getEmail(), user.getDeletedAt());
    }
}