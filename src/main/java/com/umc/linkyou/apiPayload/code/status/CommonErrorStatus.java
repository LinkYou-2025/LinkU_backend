package com.umc.linkyou.apiPayload.code.status;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorStatus implements BaseErrorCode {

    // 공통 HTTP 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON429", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // S3 관련 에러
    _S3_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S3404", "S3 파일을 찾을 수 없습니다."),
    _S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35001", "S3 파일 업로드에 실패했습니다."),
    _S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35002", "S3 파일 삭제에 실패했습니다."),
    _S3_INVALID_FILE(HttpStatus.BAD_REQUEST, "S34001", "유효하지 않은 파일입니다."),
    _S3_INVALID_IMAGE(HttpStatus.BAD_REQUEST, "S34002", "이미지 파일만 업로드할 수 있습니다."),
    _S3_INVALID_URL(HttpStatus.BAD_REQUEST, "S34003", "유효하지 않은 S3 URL입니다."),
    _S3_FILE_EMPTY(HttpStatus.BAD_REQUEST, "S34004", "업로드할 파일이 없습니다."),
    _S3_EXTRACT_URL_FAILED(HttpStatus.BAD_REQUEST, "S34005", "URL에서 파일명을 추출할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
