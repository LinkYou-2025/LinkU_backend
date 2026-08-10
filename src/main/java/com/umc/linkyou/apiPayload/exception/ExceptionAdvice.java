package com.umc.linkyou.apiPayload.exception;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ConstraintViolationException 추출 도중 에러 발생"));

        log.warn("[Validation Error] {}", errorMessage, e);
        return handleExceptionInternalConstraint(e, ErrorStatus.valueOf(errorMessage), HttpHeaders.EMPTY,request);
    }
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                               @NonNull HttpHeaders headers,
                                                               @NonNull HttpStatusCode status,
                                                               @NonNull WebRequest request) {

        Map<String, String> errors = extractFieldErrors(e.getBindingResult());

        log.warn("[Method Argument Not Valid] Errors: {}", errors, e);
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, request, errors);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        Map<String, String> errors = Map.of(e.getName(), "형식이 올바르지 않습니다.");

        log.warn("[Method Argument Type Mismatch] Parameter: {}, Value: {}", e.getName(), e.getValue(), e);
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, request, errors);
    }

    // 요청 바디 파싱 실패 (malformed JSON 등) — Jackson 내부 메시지 노출 차단
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        log.warn("[HttpMessageNotReadableException] {}", e.getMessage());
        return handleExceptionInternalConstraint(e, CommonErrorStatus._BAD_REQUEST, headers, request);
    }

    // DB unique 제약조건 위반 → 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e, WebRequest request) {
        log.warn("[DataIntegrityViolationException] {}", e.getMostSpecificCause().getMessage());
        return handleExceptionInternalConstraint(e, CommonErrorStatus._CONFLICT, HttpHeaders.EMPTY, request);
    }

    // @RequestParam 누락
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e,
                                                                          @NonNull HttpHeaders headers,
                                                                          @NonNull HttpStatusCode status,
                                                                          @NonNull WebRequest request) {
        Map<String, String> errors = Map.of(e.getParameterName(), "필수 파라미터입니다.");

        log.warn("[MissingServletRequestParameterException] Parameter: {}", e.getParameterName());
        return handleExceptionInternalArgs(e, headers, request, errors);
    }

    // @ModelAttribute 바인딩 실패 (MethodArgumentNotValidException이 잡지 못하는 케이스)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException e, WebRequest request) {
        Map<String, String> errors = extractFieldErrors(e.getBindingResult());

        log.warn("[BindException] Errors: {}", errors);
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, request, errors);
    }

    // 요청 데이터 포맷 오류 (Jackson)
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Object> handleInvalidFormat(InvalidFormatException e, WebRequest request) {
        log.warn("[InvalidFormatException] targetType: {}, value: {}", e.getTargetType(), e.getValue());
        return handleExceptionInternalConstraint(e, CommonErrorStatus._BAD_REQUEST, HttpHeaders.EMPTY, request);
    }

    // @PreAuthorize 등 인가 실패
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException e, WebRequest request) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return handleExceptionInternalConstraint(e, AuthErrorStatus.PERMISSION_DENIED, HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        log.error("[Unhandled Exception] {}", e.getMessage(), e);

        return handleExceptionInternalFalse(e, request, e.getMessage());
    }



    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity onThrowException(GeneralException generalException, HttpServletRequest request) {
        ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
        log.error("[GeneralException] Code: {}, Message: {}, URI: {}", 
                errorReasonHttpStatus.getCode(), 
                errorReasonHttpStatus.getMessage(),
                request.getRequestURI(), 
                generalException);
        return handleExceptionInternal(generalException,errorReasonHttpStatus, request);
    }

    // 필드별 검증 에러를 필드명 -> 메시지 맵으로 변환 (한 필드에 에러가 여러 개면 콤마로 이어붙임)
    private Map<String, String> extractFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse(""),
                        (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage,
                        LinkedHashMap::new));
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorReasonDTO reason,
                                                           HttpServletRequest request) {

        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), Collections.emptyMap());

        log.error("[API Error] URI: {}, Method: {}, Code: {}, Message: {}",
                request.getRequestURI(),
                request.getMethod(),
                reason.getCode(),
                reason.getMessage(),
                e);

        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(
                e,
                body,
                null,
                reason.getHttpStatus(),
                webRequest
        );
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(Exception e, WebRequest request, String errorPoint) {
        var reason = CommonErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();
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

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY, reason.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(Exception e, HttpHeaders headers,
                                                               WebRequest request, Map<String, String> errorArgs) {
        var reason = CommonErrorStatus._BAD_REQUEST.getReasonHttpStatus();
        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), errorArgs);

        if (request instanceof ServletWebRequest servletRequest) {
            log.warn("[API Validation Error] URI: {}, Method: {}, Code: {}, Message: {}, Errors: {}",
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
        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), Collections.emptyMap());

        if (request instanceof ServletWebRequest servletRequest) {
            log.warn("[API Constraint Error] URI: {}, Method: {}, Code: {}, Message: {}",
                    servletRequest.getRequest().getRequestURI(),
                    servletRequest.getRequest().getMethod(),
                    reason.getCode(),
                    reason.getMessage(),
                    e);
        }

        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), request);
    }

}
