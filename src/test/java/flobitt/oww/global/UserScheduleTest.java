package flobitt.oww.global;

import flobitt.oww.domain.user.service.UserService;
import flobitt.oww.global.properties.SchedulerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSchedulerTest {

    @Mock
    private UserService userService;

    @Mock
    private SchedulerProperties schedulerProperties;

    @InjectMocks
    private UserScheduler userScheduler;

    @Test
    @DisplayName("미인증 사용자 정리 작업 성공")
    void cleanupUnverifiedUsers_Success() {
        // given
        given(userService.deleteExpiredUnverifiedUsers()).willReturn(5);
        given(schedulerProperties.getMaxRetries()).willReturn(3);

        // when
        userScheduler.cleanupUnverifiedUsers();

        // then
        verify(userService, times(1)).deleteExpiredUnverifiedUsers();
    }

    @Test
    @DisplayName("미인증 사용자 정리 작업 실패 후 재시도 성공")
    void cleanupUnverifiedUsers_RetrySuccess() {
        // given
        given(schedulerProperties.getMaxRetries()).willReturn(3);
        given(userService.deleteExpiredUnverifiedUsers())
                .willThrow(new RuntimeException("Database connection failed"))
                .willReturn(3); // 두 번째 시도에서 성공

        // when
        userScheduler.cleanupUnverifiedUsers();

        // then
        verify(userService, times(2)).deleteExpiredUnverifiedUsers();
    }

    @Test
    @DisplayName("미인증 사용자 정리 작업 최대 재시도 후 실패")
    void cleanupUnverifiedUsers_MaxRetriesExceeded() {
        // given
        given(schedulerProperties.getMaxRetries()).willReturn(2);
        given(userService.deleteExpiredUnverifiedUsers())
                .willThrow(new RuntimeException("Persistent error"));

        // when
        userScheduler.cleanupUnverifiedUsers();

        // then
        verify(userService, times(2)).deleteExpiredUnverifiedUsers();
    }

    @Test
    @DisplayName("삭제된 사용자 완전 삭제 작업 성공")
    void hardDeleteExpiredDeletedUsers_Success() {
        // given
        given(userService.hardDeleteExpiredDeletedUsers()).willReturn(2);
        given(schedulerProperties.getMaxRetries()).willReturn(3);

        // when
        userScheduler.hardDeleteExpiredDeletedUsers();

        // then
        verify(userService, times(1)).hardDeleteExpiredDeletedUsers();
    }

    @Test
    @DisplayName("삭제된 사용자 완전 삭제 작업 실패 후 재시도 성공")
    void hardDeleteExpiredDeletedUsers_RetrySuccess() {
        // given
        given(schedulerProperties.getMaxRetries()).willReturn(3);
        given(userService.hardDeleteExpiredDeletedUsers())
                .willThrow(new RuntimeException("Database lock timeout"))
                .willThrow(new RuntimeException("Database lock timeout"))
                .willReturn(1); // 세 번째 시도에서 성공

        // when
        userScheduler.hardDeleteExpiredDeletedUsers();

        // then
        verify(userService, times(3)).hardDeleteExpiredDeletedUsers();
    }
}
