package flobitt.oww.api;

import flobitt.oww.api.in.EmailVerificationAPI;
import flobitt.oww.domain.user.dto.req.ResendEmailReq;
import flobitt.oww.domain.user.service.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController implements EmailVerificationAPI {

    private final AuthFacade authFacade;

    @GetMapping("/{token}")
    public ResponseEntity<Void> verifyEmail(@PathVariable String token) {
        authFacade.verifyEmail(token);
        return  ResponseEntity.status(OK).build();
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resendEmail(@RequestBody ResendEmailReq req) {
        authFacade.resendEmail(req);
        return  ResponseEntity.status(OK).build();
    }
}