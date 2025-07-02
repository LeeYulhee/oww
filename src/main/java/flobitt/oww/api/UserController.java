package flobitt.oww.api;

import flobitt.oww.api.in.UserAPI;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.service.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final AuthFacade authFacade;

    @PostMapping
    public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserReq req) {
        authFacade.signUp(req);
        return  ResponseEntity.status(CREATED).build();
    }
}
