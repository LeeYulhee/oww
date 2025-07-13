package flobitt.oww.support;

import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import flobitt.oww.domain.user.entity.VerificationType;

import java.time.LocalDateTime;

/**
 * 테스트용 객체 생성을 위한 Fixture 클래스
 */
public class TestFixtures {

    public static User createUser(String email, String loginId) {
        return User.builder()
                .userLoginId(loginId)
                .email(email)
                .password("encodedPassword123!")
                .userStatus(UserStatus.NOT_VERIFIED)
                .build();
    }

    public static User createVerifiedUser(String email, String loginId) {
        User user = createUser(email, loginId);
        user.updateUserStatusActive();
        return user;
    }

    public static User createDeletedUser(String email, String loginId) {
        User user = createUser(email, loginId);
        user.delete();
        return user;
    }

    public static EmailVerification createEmailVerification(User user, String token) {
        return createEmailVerification(user, token, VerificationType.SIGNUP);
    }

    public static EmailVerification createEmailVerification(User user, String token, VerificationType type) {
        return EmailVerification.builder()
                .verificationToken(token)
                .verificationType(type)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();
    }

    public static EmailVerification createExpiredEmailVerification(User user, String token) {
        return EmailVerification.builder()
                .verificationToken(token)
                .verificationType(VerificationType.SIGNUP)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().minusHours(1)) // 이미 만료됨
                .user(user)
                .build();
    }

    public static EmailVerification createVerifiedEmailVerification(User user, String token) {
        EmailVerification verification = createEmailVerification(user, token);
        verification.updateEmailVerification();
        return verification;
    }
}