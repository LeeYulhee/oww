package flobitt.oww.domain.user.repository;

import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> findExpiredUnverifiedUsers(UserStatus userStatus, LocalDateTime cutoffTime);
    List<User> findExpiredDeletedUsers(LocalDateTime cutoffTime);
}
