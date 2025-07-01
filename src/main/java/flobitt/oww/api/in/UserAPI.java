package flobitt.oww.api.in;

import flobitt.oww.domain.user.dto.req.CreateUserReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "users", description = "user API")
public interface UserAPI {

    @Operation(summary = "회원가입", description = "회원가입을 한다.")
    public ResponseEntity<Void> createUser(CreateUserReq dto);
}
