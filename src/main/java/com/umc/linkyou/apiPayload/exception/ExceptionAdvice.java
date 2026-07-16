package com.umc.linkyou.apiPayload.exception;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ConstraintViolationException 추출 도중 에러 발생"));

        log.error("[Validation Error] {}", errorMessage, e);
        return handleExceptionInternalConstraint(e, ErrorStatus.valueOf(errorMessage), HttpHeaders.EMPTY,request);
    }
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors().stream()
                .forEach(fieldError -> {
                    String fieldName = fieldError.getField();
                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                    errors.merge(fieldName, errorMessage, (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
                });

        log.error("[Method Argument Not Valid] Errors: {}", errors, e);
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, CommonErrorStatus._BAD_REQUEST, request, errors);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        Map<String, String> errors = Map.of(e.getName(), "형식이 올바르지 않습니다.");

        log.error("[Method Argument Type Mismatch] Parameter: {}, Value: {}", e.getName(), e.getValue(), e);
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, CommonErrorStatus._BAD_REQUEST, request, errors);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        log.error("[Unhandled Exception] {}", e.getMessage(), e);

        return handleExceptionInternalFalse(e, CommonErrorStatus._INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, CommonErrorStatus._INTERNAL_SERVER_ERROR.getHttpStatus(), request, e.getMessage());
    }



    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity onThrowException(GeneralException generalException, HttpServletRequest request) {
        ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
        log.error("[GeneralException] Code: {}, Message: {}, URI: {}", 
                errorReasonHttpStatus.getCode(), 
                errorReasonHttpStatus.getMessage(),
                request.getRequestURI(), 
                generalException);
        return handleExceptionInternal(generalException,errorReasonHttpStatus,null,request);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorReasonDTO reason,
                                                           HttpHeaders headers, HttpServletRequest request) {

        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(),reason.getMessage(),null);
        
        if (request != null) {
            log.error("[API Error] URI: {}, Method: {}, Code: {}, Message: {}", 
                    request.getRequestURI(), 
                    request.getMethod(),
                    reason.getCode(), 
                    reason.getMessage(), 
                    e);
        }

        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(
                e,
                body,
                headers,
                reason.getHttpStatus(),
                webRequest
        );
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(Exception e, BaseErrorCode errorCommonStatus,
                                                                HttpHeaders headers, HttpStatus status, WebRequest request, String errorPoint) {
        var reason = errorCommonStatus.getReasonHttpStatus();
        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), errorPoint);

        if (request instanceof ServletWebRequest servletRequest) {
            log.error("[API Error] URI: {}, Method: {}, Code: {}, Message: {}, ErrorPoint: {}",
                    servletRequest.getRequest().getRequestURI(),
                    servletRequest.getRequest().getMethod(),
                    reason.getCode(),
                    reason.getMessage(),
                    errorPoint,
                    e);
        }

        return super.handleExceptionInternal(e, body, headers, status, request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(Exception e, HttpHeaders headers, BaseErrorCode errorCommonStatus,
                                                               WebRequest request, Map<String, String> errorArgs) {
        var reason = errorCommonStatus.getReasonHttpStatus();
        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), errorArgs);

        if (request instanceof ServletWebRequest servletRequest) {
            log.error("[API Validation Error] URI: {}, Method: {}, Code: {}, Message: {}, Errors: {}",
                    servletRequest.getRequest().getRequestURI(),
                    servletRequest.getRequest().getMethod(),
                    reason.getCode(),
                    reason.getMessage(),
                    errorArgs,
                    e);
        }

        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalConstraint(Exception e, BaseErrorCode errorCommonStatus,
                                                                     HttpHeaders headers, WebRequest request) {
        var reason = errorCommonStatus.getReasonHttpStatus();
        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), null);

        if (request instanceof ServletWebRequest servletRequest) {
            log.error("[API Constraint Error] URI: {}, Method: {}, Code: {}, Message: {}",
                    servletRequest.getRequest().getRequestURI(),
                    servletRequest.getRequest().getMethod(),
                    reason.getCode(),
                    reason.getMessage(),
                    e);
        }

        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), request);
    }

}
