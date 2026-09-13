package com.umc.linkyou.apiPayload.code.status.folder;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FolderErrorStatus implements BaseErrorCode {

    // 400 Bad Request
    _FOLDER_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "FOLDER4001", "유효하지 않은 커서 값입니다."),
    _FOLDER_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "FOLDER4002", "소분류 폴더 하위에는 폴더를 생성할 수 없습니다."),

    // 403 Forbidden
    _FOLDER_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4031", "해당하는 폴더를 생성할 권한이 없습니다."),
    _FOLDER_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4032", "해당하는 폴더의 수정 권한이 없습니다."),
    _FOLDER_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4033", "해당하는 폴더의 삭제 권한이 없습니다."),
    _FOLDER_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4034", "해당 폴더에 접근 권한이 없습니다."),

    // 404 Not Found
    _FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER4041", "해당하는 폴더를 찾을 수 없습니다."),
    _FOLDER_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER4042", "폴더의 부모 폴더가 없습니다."),
    _FOLDER_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER4043", "폴더의 카테고리가 없습니다."),

    // 409 Conflict
    _FOLDER_CREATE_DUPLICATE(HttpStatus.CONFLICT, "FOLDER4091", "중복된 폴더명입니다."),
    _FOLDER_NAME_CONFLICT(HttpStatus.CONFLICT, "FOLDER4092", "카테고리명과 동일한 폴더명은 사용할 수 없습니다."),

    // 500 Internal Server Error
    _FOLDER_OWNER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "FOLDER5001", "폴더의 소유자 정보를 찾을 수 없습니다.");

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