package flobitt.oww.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import flobitt.oww.domain.user.entity.QUser;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;
    QUser user = QUser.user;

    @Override
    public List<User> findExpiredUnverifiedUsers(UserStatus userStatus, LocalDateTime cutoffTime) {
        return jpaQueryFactory
                .selectFrom(user)
                .where(
                        user.userStatus.eq(userStatus),
                        user.isDeleted.eq(false),
                        user.createdAt.lt(cutoffTime)
                )
                .fetch();
    }
}
