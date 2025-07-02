package flobitt.oww.global.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {
    /**
     * 스케줄러 활성화 여부
     */
    private boolean enabled;

    /**
     * 미인증 사용자 정리 작업 cron 표현식
     */
    private String userCleanupCron; // 매일 새벽 2시

    /**
     * 최대 재시도 횟수
     */
    private int maxRetries;
}
