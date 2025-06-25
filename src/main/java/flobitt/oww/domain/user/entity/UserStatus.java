package flobitt.oww.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UserStatus {
    ACTIVE,
    NOT_VERIFIED
}
