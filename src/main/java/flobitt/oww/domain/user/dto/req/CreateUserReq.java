package flobitt.oww.domain.user.dto.req;

import flobitt.oww.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateUserReq {

    @NotBlank(message = "로그인 ID는 필수입니다.")
    @Size(min = 4, max = 50, message = "로그인 ID는 4-50자 사이여야 합니다.")
    private String userLoginId;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    public static User toEntity(CreateUserReq req, String encodedPassword) {
        return User.builder()
                .userLoginId(req.getUserLoginId())
                .email(req.getEmail())
                .password(encodedPassword)
                .build();
    }
}