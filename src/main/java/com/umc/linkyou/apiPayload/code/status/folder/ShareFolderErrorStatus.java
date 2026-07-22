package com.umc.linkyou.apiPayload.code.status.folder;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShareFolderErrorStatus implements BaseErrorCode {

    // 400 Bad Request
    _INVALID_PERMISSION_TYPE(HttpStatus.BAD_REQUEST, "SHAREFOLDER4001", "유효하지 않은 권한 타입입니다."),

    // 403 Forbidden
    _FOLDER_PERMISSION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "SHAREFOLDER4031", "폴더 수정 권한을 가지고 있지 않습니다."),
    _FOLDER_OWNER_UPDATE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "SHAREFOLDER4032", "폴더 주인의 권한은 수정할 수 없습니다."),

    // 404 Not Found
    _FOLDER_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SHAREFOLDER4041", "해당 유저의 폴더 권한 정보를 찾을 수 없습니다."),

    // 409 Conflict
    _FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER(HttpStatus.CONFLICT, "SHAREFOLDER4091", "소유권을 위임할 다른 멤버가 없어 폴더를 나갈 수 없습니다.");

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