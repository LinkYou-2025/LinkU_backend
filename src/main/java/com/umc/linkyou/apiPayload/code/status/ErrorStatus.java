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
    _EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST,"COMMON400","인증 코드가 만료되었습니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON4011", "잘못된 토큰입니다."),
    _INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "COMMON4012", "잘못된 비밀번호입니다."),

    // S3 관련 오류
    _S3_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S3404", "S3 파일을 찾을 수 없습니다."),
    _S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35001", "S3 파일 업로드에 실패했습니다."),
    _S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S35002", "S3 파일 삭제에 실패했습니다."),
    _S3_INVALID_FILE(HttpStatus.BAD_REQUEST, "S34001", "유효하지 않은 파일입니다."),
    _S3_INVALID_IMAGE(HttpStatus.BAD_REQUEST, "S34002", "이미지 파일만 업로드할 수 있습니다."),
    _S3_INVALID_URL(HttpStatus.BAD_REQUEST, "S34003", "유효하지 않은 S3 URL입니다."),
    _S3_FILE_EMPTY(HttpStatus.BAD_REQUEST, "S34004", "업로드할 파일이 없습니다."),
    _S3_EXTRACT_URL_FAILED(HttpStatus.BAD_REQUEST, "S34005", "URL에서 파일명을 추출할 수 없습니다."),

    // 로그인 회원가입 에러
    _LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USERS4013", "이메일 주소 또는 비밀번호를 다시 확인하세요."),
    _DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USERS403", "중복된 닉네임입니다."),
    _DUPLICATE_JOIN_REQUEST(HttpStatus.CONFLICT, "USERS403", "중복된 이메일입니다."),
    _VERIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USERS404", "인증 코드 검증 실패"),
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USERS404", "사용자를 찾을 수 없습니다."),
    _PURPOSE_NOT_PROVIDED(HttpStatus.NOT_FOUND, "USERS404", "목적을 선택해야합니다."),
    _INTEREST_NOT_PROVIDED(HttpStatus.NOT_FOUND, "USERS404", "관심 분야를 선택해야합니다."),
    _NO_SUCH_ALGORITHM(HttpStatus.INTERNAL_SERVER_ERROR, "USERS500", "인증 코드 생성 실패"),
    _SEND_MAIL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USERS500", "인증 코드 전송 실패"),
    //소셜로그인 관련
    // 소셜로그인 관련 추가/수정
    _AUTH_ACCOUNT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USERS5001", "소셜 계정 연결에 실패했습니다."),
    _USER_SOCIAL_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USERS5002", "소셜 사용자 생성에 실패했습니다."),
    // 소셜 로그인 섹션에 추가
    _SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "USERS4003", "소셜 로그인에 이메일이 필요합니다."),
    _SOCIAL_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "USERS4004", "지원하지 않는 소셜 제공자입니다."),


    //링큐 관련 코드
    _LINKU_VIDEO_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "LINKU4001", "영상 링크는 저장할 수 없습니다."),
    _LINKU_INVALID_URL(HttpStatus.BAD_REQUEST, "LINKU4002", "유효하지 않은 링크입니다."),
    _USER_LINKU_NOT_FOUND(HttpStatus.NOT_FOUND, "LINKU404", "user_linku 테이블을 찾기 못했습니다." ),
    //OPENAI관련 오류
    _AI_PARSE_ERROR(HttpStatus.BAD_REQUEST, "OPENAI5001", "AI 응답 파싱에 실패했습니다."),
    _AI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI5002", "AI 응답이 예상한 형식이 아닙니다."),

    //AIArticle 생성오류
    _DUPLICATE_AI_ARTICLE(HttpStatus.CONFLICT, "AIARTICLE4091", "이미 해당 링크로 생성된 AI Article이 존재합니다."),
    _AI_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "AIARTICLE4041", "해당하는 AI Article을 찾을 수 없습니다."),
    _CONTENT_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CRAWLER5001", "웹페이지 본문 추출에 실패했습니다."),
    _CONTENT_EXTRACTION_PROHIBITED(HttpStatus.INTERNAL_SERVER_ERROR, "CRAWLER5002", "크롤링이 금지된 웹사이트입니다."),
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

    // 폴더 관련 오류
    _FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER404", "해당하는 폴더를 찾을 수 없습니다."),
    _FOLDER_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_PARENT404", "폴더의 부모 폴더가 없습니다."),
    _FOLDER_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_CATEGORY404", "폴더의 카테고리가 없습니다."),
    _FOLDER_CREATE_DUPLICATE(HttpStatus.CONFLICT, "FOLDER_CATEGORY404", "중복된 폴더명입니다."),
    _FOLDER_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER_UPDATE403", "해당하는 폴더의 수정 권한이 없습니다."),
    _FOLDER_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER_DELETE403", "해당하는 폴더의 삭제 권한이 없습니다."),

    // 공유 폴더 관련 응답
    _FOLDER_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_PERMISSION404", "해당 유저의 폴더 권한 정보를 찾을 수 없습니다."),
    _FOLDER_PERMISSION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "FOLDER_OWNER_403", "폴더 수정 권한을 가지고 있지 않습니다."),
    _FOLDER_OWNER_UPDATE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "FOLDER_OWNER_403", "폴더 주인의 권한은 수정할 수 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_TOKEN404", "공유 폴더 토큰을 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.NOT_FOUND, "FOLDER_TOKEN_INVALID404", "공유 폴더 토큰이 유효하지 않습니다."),
    INVITATION_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_LINK_INVALID404", "공유 폴더 링크가 유효하지 않습니다."),
    _INVALID_PERMISSION_TYPE(HttpStatus.BAD_REQUEST, "PERMISSION400", "유효하지 않은 권한 타입입니다."),
    INVITATION_CREATOR_CANNOT_ACCEPT(HttpStatus.FORBIDDEN, "FOLDER_CREATOR403", "초대 생성자는 자신의 링크로 참여할 수 없습니다."),
    // 북마크 관련 오류
    _FOLDER_BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_BOOKMARK404", "해당 유저의 북마크 정보가 존재하지 않습니다."),
    ;

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
