package flobitt.oww.domain.user.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ParseTokenDto {
    private final String userId;
    private final String email;
    private final String tokenType;
}
