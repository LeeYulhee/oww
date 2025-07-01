package flobitt.oww.domain.user.dto.internal;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ParseTokenDto {
    private final String userId;
    private final String email;
    private final String tokenType;
}
