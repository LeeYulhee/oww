package flobitt.oww.domain.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SendVerificationEmailEvent {
    private String email;
    private String token;
}
