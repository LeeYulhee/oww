package flobitt.oww.api;

import flobitt.oww.api.in.UserAPI;
import flobitt.oww.domain.user.dto.req.CreateUserReq;
import flobitt.oww.domain.user.service.AuthFacade;
import flobitt.oww.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

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
