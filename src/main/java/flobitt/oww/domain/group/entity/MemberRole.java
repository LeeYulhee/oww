package flobitt.oww.domain.group.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT) // 이건 뭔지 공부해보기
public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER;
}
