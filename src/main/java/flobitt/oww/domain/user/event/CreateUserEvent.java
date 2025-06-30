package flobitt.oww.domain.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateUserEvent {
    private String email;
    private String token;
}
