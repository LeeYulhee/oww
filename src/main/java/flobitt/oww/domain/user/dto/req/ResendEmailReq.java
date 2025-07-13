package flobitt.oww.domain.user.dto.req;

import flobitt.oww.domain.user.entity.VerificationType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResendEmailReq {

    private String email;
    private VerificationType type;
}
