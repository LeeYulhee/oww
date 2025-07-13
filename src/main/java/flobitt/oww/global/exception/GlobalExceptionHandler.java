package flobitt.oww.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static flobitt.oww.global.exception.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.getStatus())
                .divisionCode(INTERNAL_SERVER_ERROR.getDivisionCode())
                .resultMsg(INTERNAL_SERVER_ERROR.getMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(NullPointerException.class)
    protected ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(NULL_POINT_ERROR.getStatus())
                .divisionCode(NULL_POINT_ERROR.getDivisionCode())
                .resultMsg(NULL_POINT_ERROR.getMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    protected ResponseEntity<ErrorResponse> handleBadRequestException(HttpClientErrorException e) {
        log.error("HttpClientErrorException", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(BAD_REQUEST_ERROR.getStatus())
                .divisionCode(BAD_REQUEST_ERROR.getDivisionCode())
                .resultMsg(BAD_REQUEST_ERROR.getMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNotFoundException(NoResourceFoundException e) {
        log.error("NoResourceFoundException", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(NOT_FOUND_ERROR.getStatus())
                .divisionCode(NOT_FOUND_ERROR.getDivisionCode())
                .resultMsg(NOT_FOUND_ERROR.getMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.valueOf(e.getStatusCode().value()))
                .divisionCode(BAD_REQUEST_ERROR.getDivisionCode())
                .resultMsg(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(BAD_REQUEST_ERROR.getStatus())
                .divisionCode(BAD_REQUEST_ERROR.getDivisionCode())
                .resultMsg(BAD_REQUEST_ERROR.getMessage())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * Custom Exception
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException", e);

        final ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(errorResponse);
    }

    /**
     * Message + Custom Exception
     */
    @ExceptionHandler(BusinessMessageException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessMessageException(BusinessMessageException e) {
        log.error("BusinessMessageException", e);
        final ErrorCode errorCode = e.getErrorCode();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(errorCode.getStatus())
                .divisionCode(errorCode.getDivisionCode())
                .resultMsg(e.getMessage() + errorCode.getMessage())
                .build();

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(errorResponse);
    }
}