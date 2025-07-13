package flobitt.oww.domain.group.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
// @JsonFormat(shape = JsonFormat.Shape.OBJECT) : Jackson에서 enum을 JSON으로 변환할 때 어떤 형태로 만들지 정하는 어노테이션, OBJECT로 설정하면 객체로 직렬화
public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER;
}
