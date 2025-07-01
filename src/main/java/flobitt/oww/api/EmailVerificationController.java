package flobitt.oww.api;

import flobitt.oww.domain.user.service.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final AuthFacade authFacade;

    @GetMapping("/{token}")
    public ResponseEntity<Void> verifyEmail(@PathVariable String token) {
        log.info("@@@@@@@@이메일 인증 = {}", token);
        authFacade.verifyEmail(token);
        return  ResponseEntity.status(OK).build();
    }
}
