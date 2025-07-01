package flobitt.oww.api.in;

import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface EmailVerificationAPI {

    @Operation(summary = "이메일 인증", description = "이메일 인증을 진행한다.")
    public ResponseEntity<Void> verifyEmail(String token);

    @Operation(summary = "인증 이메일 재전송", description = "인증 이메일을 재전송한다.")
    public ResponseEntity<Void> resendEmail(ResendEmailReq req);
}
