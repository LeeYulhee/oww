package flobitt.oww.support;

import flobitt.oww.domain.user.entity.EmailVerification;
import flobitt.oww.domain.user.entity.User;
import flobitt.oww.domain.user.entity.UserStatus;
import org.assertj.core.api.AbstractAssert;

import java.time.LocalDateTime;

/**
 * 커스텀 Assertion 클래스들
 */
public class CustomAssertions {

    public static UserAssert assertThat(User actual) {
        return new UserAssert(actual);
    }

    public static EmailVerificationAssert assertThat(EmailVerification actual) {
        return new EmailVerificationAssert(actual);
    }

    public static class UserAssert extends AbstractAssert<UserAssert, User> {

        public UserAssert(User actual) {
            super(actual, UserAssert.class);
        }

        public UserAssert hasEmail(String email) {
            isNotNull();
            if (!actual.getEmail().equals(email)) {
                failWithMessage("Expected user's email to be <%s> but was <%s>",
                        email, actual.getEmail());
            }
            return this;
        }

        public UserAssert hasLoginId(String loginId) {
            isNotNull();
            if (!actual.getUserLoginId().equals(loginId)) {
                failWithMessage("Expected user's login ID to be <%s> but was <%s>",
                        loginId, actual.getUserLoginId());
            }
            return this;
        }

        public UserAssert isActive() {
            isNotNull();
            if (actual.getUserStatus() != UserStatus.ACTIVE) {
                failWithMessage("Expected user to be ACTIVE but was <%s>",
                        actual.getUserStatus());
            }
            if (actual.getEmailVerifiedAt() == null) {
                failWithMessage("Expected user to have emailVerifiedAt but was null");
            }
            return this;
        }

        public UserAssert isNotVerified() {
            isNotNull();
            if (actual.getUserStatus() != UserStatus.NOT_VERIFIED) {
                failWithMessage("Expected user to be NOT_VERIFIED but was <%s>",
                        actual.getUserStatus());
            }
            return this;
        }

        public UserAssert isDeleted() {
            isNotNull();
            if (!actual.isDeleted()) {
                failWithMessage("Expected user to be deleted but was not");
            }
            if (actual.getDeletedAt() == null) {
                failWithMessage("Expected user to have deletedAt but was null");
            }
            return this;
        }

        public UserAssert isNotDeleted() {
            isNotNull();
            if (actual.isDeleted()) {
                failWithMessage("Expected user to not be deleted but was deleted");
            }
            return this;
        }
    }

    public static class EmailVerificationAssert extends AbstractAssert<EmailVerificationAssert, EmailVerification> {

        public EmailVerificationAssert(EmailVerification actual) {
            super(actual, EmailVerificationAssert.class);
        }

        public EmailVerificationAssert hasToken(String token) {
            isNotNull();
            if (!actual.getVerificationToken().equals(token)) {
                failWithMessage("Expected verification token to be <%s> but was <%s>",
                        token, actual.getVerificationToken());
            }
            return this;
        }

        public EmailVerificationAssert isVerified() {
            isNotNull();
            if (actual.getVerifiedAt() == null) {
                failWithMessage("Expected verification to be verified but verifiedAt was null");
            }
            return this;
        }

        public EmailVerificationAssert isNotVerified() {
            isNotNull();
            if (actual.getVerifiedAt() != null) {
                failWithMessage("Expected verification to not be verified but verifiedAt was <%s>",
                        actual.getVerifiedAt());
            }
            return this;
        }

        public EmailVerificationAssert isExpired() {
            isNotNull();
            if (actual.getExpiresAt().isAfter(LocalDateTime.now())) {
                failWithMessage("Expected verification to be expired but expiresAt was <%s>",
                        actual.getExpiresAt());
            }
            return this;
        }

        public EmailVerificationAssert isNotExpired() {
            isNotNull();
            if (actual.getExpiresAt().isBefore(LocalDateTime.now())) {
                failWithMessage("Expected verification to not be expired but expiresAt was <%s>",
                        actual.getExpiresAt());
            }
            return this;
        }
    }
}