package flobitt.oww.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode {

    /**
     * 400
     */
    BAD_REQUEST_ERROR(BAD_REQUEST, "G001", "Bad Request Exception"),
    INVALID_TYPE_VALUE(BAD_REQUEST, "G002", " Invalid Type Value"),
    IO_ERROR(BAD_REQUEST, "G003", "I/O Exception"),
    JSON_PARSE_ERROR(BAD_REQUEST, "G004", "JsonParseException"),

    /**
     * 403
     */
    FORBIDDEN_ERROR(FORBIDDEN, "G001", "Forbidden Exception"),

    /**
     * 404
     */
    NOT_FOUND_ERROR(NOT_FOUND, "G001", "Not Found Exception"),
    NULL_POINT_ERROR(NOT_FOUND, "G002", "Null Point Exception"),

    /**
     * 500
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "Internal Server Error Exception")
    ;

    private final HttpStatus status;
    private final String divisionCode;
    private final String message;

    ErrorCode(final HttpStatus status, final String divisionCode, final String message) {
        this.status = status;
        this.divisionCode = divisionCode;
        this.message = message;
    }
}
