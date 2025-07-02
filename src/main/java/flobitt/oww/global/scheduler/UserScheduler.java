package flobitt.oww.global.scheduler;

import flobitt.oww.domain.user.service.UserService;
import flobitt.oww.global.properties.SchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class UserScheduler {

    private final UserService userService;
    private final SchedulerProperties schedulerProperties;

    /**
     * 미인증 사용자 정리 작업 스케줄링
     */
    @Scheduled(cron = "#{@schedulerProperties.userCleanupCron}")
    public void cleanupUnverifiedUsers() {
        log.info("미인증 사용자 정리 작업 시작");

        try {
            int deletedCount = executeWithRetry(userService::deleteExpiredUnverifiedUsers);
            log.info("미인증 사용자 정리 작업 완료: {}명 처리", deletedCount);

        } catch (Exception e) {
            log.error("미인증 사용자 정리 작업 최종 실패", e);
        }
    }

    /**
     * 삭제된 사용자 완전 삭제 작업 스케줄링
     */
    @Scheduled(cron = "#{@schedulerProperties.userHardDeleteCron}")
    public void hardDeleteExpiredDeletedUsers() {
        log.info("삭제된 사용자 완전 삭제 작업 시작");

        try {
            int deletedCount = executeWithRetry(userService::hardDeleteExpiredDeletedUsers);
            log.info("삭제된 사용자 완전 삭제 작업 완료: {}명 처리", deletedCount);

        } catch (Exception e) {
            log.error("삭제된 사용자 완전 삭제 작업 최종 실패", e);
        }
    }

    /**
     * 재시도 로직이 포함된 작업 실행
     */
    private int executeWithRetry(SchedulerTask task) throws Exception {
        int maxRetries = Math.max(1, schedulerProperties.getMaxRetries());

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return task.execute();
            } catch (Exception e) {
                log.warn("스케줄러 작업 실패 (시도: {}/{}): {}",
                        attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    throw e;
                }

                waitBeforeRetry(attempt);
            }
        }

        throw new RuntimeException("예상치 못한 실행 경로");
    }

    /**
     * 재시도 전 대기
     */
    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(5000 * attempt); // 점진적 대기(Progressive Backoff) 전략
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 대기 중 인터럽트 발생", e);
        }
    }

    /**
     * 스케줄러 작업을 위한 함수형 인터페이스
     */
    @FunctionalInterface
    private interface SchedulerTask {
        int execute() throws Exception;
    }
}