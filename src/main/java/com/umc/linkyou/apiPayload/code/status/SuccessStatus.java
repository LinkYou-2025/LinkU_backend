package com.umc.linkyou.apiPayload.code.status;

import com.umc.linkyou.apiPayload.code.BaseCode;
import com.umc.linkyou.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    // 일반적인 응답
    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    // 생성 관련 응답
    _CREATED(HttpStatus.CREATED, "COMMON201", "성공적으로 생성(저장)되었습니다."),
    // 회원가입 관련 응답
    _VERIFICATION_CODE_SENT(HttpStatus.ACCEPTED, "EMAIL202", "인증 코드 전송 성공."),
    _NICKNAME_AVAILABLE(HttpStatus.ACCEPTED, "NICKNAME202", "닉네임 중복 확인 성공."),
    _EMAIL_VERIFICATION_SUCCESS(HttpStatus.ACCEPTED, "EMAIL202", "이메일 인증 성공."),
    // 로그인 관련 응답
    _TEMP_PASSWORD_SENT(HttpStatus.ACCEPTED, "USER205", "임시 비밀번호 전송 성공."),

    //링크 생성
    _LINKU_SUS_URL(HttpStatus.ACCEPTED, "LINKU_SUS200", "유효하지 않은 링크일 가능성이 있습니다."),
    // 카테고리 관련 응답
    _CATEGORY_OK(HttpStatus.ACCEPTED, "CATEGORY200", "카테고리 조회에 성공했습니다."),
    _CATEGORY_COLOR_OK(HttpStatus.ACCEPTED, "CATEGORY_COLOR200", "카테고리 색상 설정을 성공했습니다."),

    // 폴더 관련 응답
    _FOLDER_OK(HttpStatus.ACCEPTED, "FOLDER200", "내 폴더 트리 조회르 성공"),
    _FOLDER_PARENT_OK(HttpStatus.ACCEPTED, "FOLDER_PARENT200", "내 중분류 폴더 조회를 성공했습니다"),
    _FOLDER_SUBFOLDER_OK(HttpStatus.ACCEPTED, "FOLDER_SUBFOLDER200", "하위 폴더(소분류) 조회를 성공했습니다"),
    _FOLDER_CREATE_OK(HttpStatus.ACCEPTED, "FOLDER_CREATE200", "소분류 폴더 생성에 성공했습니다."),
    _FOLDER_UPDATE_OK(HttpStatus.ACCEPTED, "FOLDER_UPDATE200", "소분류 폴더명 수정에 성공했습니다."),
    _FOLDER_LINK_OK(HttpStatus.ACCEPTED, "FOLDER_LINK200", "폴더 내부 소분류 폴더+링크 목록 조회를 성공했습니다."),

    // 공유 폴더 관련 응답
    _FOLDER_SHARE_OK(HttpStatus.ACCEPTED, "FOLDER_SHARE200", "폴더를 공유했습니다."),
    _FOLDER_UNSHARE_OK(HttpStatus.ACCEPTED, "FOLDER_UNSHARE200", "폴더를 비공개로 전환했습니다."),
    _FOLDER_SHARED_OK(HttpStatus.ACCEPTED, "FOLDER_SHARED200", "공유 받은 폴더 목록 조회를 성공했습니다."),
    _FOLDER_MEMBERS_OK(HttpStatus.ACCEPTED, "FOLDER_MEMBERS200", "폴더 뷰어 목록 조회를 성공했습니다"),
    _FOLDER_PERMISSION_OK(HttpStatus.ACCEPTED, "FOLDER_PERMISSION200", "폴더 권한 수정을 성공했습니다."),

    // 북마크 관련 응답
    _FOLDER_BOOKMARK_OK(HttpStatus.ACCEPTED, "FOLDER_BOOKMARK200", "폴더 북마크 상태 변경을 성공했습니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
