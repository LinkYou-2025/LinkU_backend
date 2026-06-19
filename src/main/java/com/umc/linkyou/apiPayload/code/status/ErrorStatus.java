package com.umc.linkyou.apiPayload.code.status;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON429", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // S3 관련 오류
    _S3_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S3404", "S3 파일을 찾을 수 없습니다."),
    _S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35001", "S3 파일 업로드에 실패했습니다."),
    _S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35002", "S3 파일 삭제에 실패했습니다."),
    _S3_INVALID_FILE(HttpStatus.BAD_REQUEST, "S34001", "유효하지 않은 파일입니다."),
    _S3_INVALID_IMAGE(HttpStatus.BAD_REQUEST, "S34002", "이미지 파일만 업로드할 수 있습니다."),
    _S3_INVALID_URL(HttpStatus.BAD_REQUEST, "S34003", "유효하지 않은 S3 URL입니다."),
    _S3_FILE_EMPTY(HttpStatus.BAD_REQUEST, "S34004", "업로드할 파일이 없습니다."),
    _S3_EXTRACT_URL_FAILED(HttpStatus.BAD_REQUEST, "S34005", "URL에서 파일명을 추출할 수 없습니다."),

    //소셜로그인 관련
    _AUTH_ACCOUNT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH5001", "소셜 계정 연결에 실패했습니다."),
    _SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "OAUTH4003", "소셜 로그인에 이메일이 필요합니다."),
    _SOCIAL_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH4004", "지원하지 않는 소셜 제공자입니다."),
    _INVALID_ID_TOKEN(HttpStatus.BAD_REQUEST, "OAUTH4008", "유효하지 않거나 만료된 ID 토큰"),
    _SOCIAL_ACCOUNT_ONLY(HttpStatus.UNAUTHORIZED, "USERS4014", "소셜 전용 계정입니다. 소셜 로그인을 이용하세요."),
    _ALREADY_ACTIVE_USER(HttpStatus.BAD_REQUEST, "USERS4001", "이미 해당 이메일로 user가 존재합니다. 새로운 회원정보를 만들고 싶다면 탈퇴해주세요."),
    _SOCIAL_EXTERNAL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "OAUTH4009", "소셜 계정 ID가 필요합니다."),
    _INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "OAUTH4010", "올바른 이메일 형식이 아닙니다."),

    //카테고리(폴더종류) 관련 에러
    _CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY4041", "해당하는 카테고리를 찾을 수 없습니다."),
    //감정 관련 에러
    _EMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "EMOTION4041", "해당하는 감정을 찾을 수 없습니다."),
    //도메인 관련 에러
    _DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "DOMAIN4041", "해당하는 도메인을 찾을 수 없습니다."),

    //Situation 상황 관련 오류
    _SITUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Situation4041", "해당하는 상황을 찾을 수 없습니다."),
    // 링크 추천 관련 에러
    _RECOMMEND_LINKU_NOT_ENOUGH_LINKS(HttpStatus.BAD_REQUEST, "LINKU4003", "추천을 위해 저장된 링크가 3개 이상이어야 합니다."),
    _RECOMMEND_LINKU_NO_RECOMMENDATION(HttpStatus.BAD_REQUEST, "LINKU4004", "추천할 만한 링크가 없습니다."),
    _RECOMMEND_LINKU_NEW_USER(HttpStatus.BAD_REQUEST, "LINKU4005", "신규 사용자는 추천 기능을 이용할 수 없습니다."),

    // 북마크 관련 오류
    _FOLDER_BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_BOOKMARK404", "해당 유저의 북마크 정보가 존재하지 않습니다."),;

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
                .build()
                ;
    }
}
