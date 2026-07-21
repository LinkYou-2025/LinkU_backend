package com.umc.linkyou.apiPayload.code.status.linku;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LinkuSuccessStatus implements BaseSuccessCode {

    LINKU_CREATED(HttpStatus.OK, "LINKU2001", "링크가 저장되었습니다."),
    LINKU_SUSPICIOUS_URL(HttpStatus.ACCEPTED, "LINKU2002", "유효하지 않은 링크일 가능성이 있습니다."),
    LINKU_RECENT_OK(HttpStatus.OK, "LINKU2003", "최근 열람한 링크를 가져왔습니다."),
    LINKU_UPDATED(HttpStatus.OK, "LINKU2004", "링크 수정에 성공했습니다."),
    LINKU_SEARCH_OK(HttpStatus.OK, "LINKU2005", "링크 검색에 성공했습니다."),
    LINKU_DELETED(HttpStatus.OK, "LINKU2006", "링크 삭제에 성공했습니다."),
    LINKU_FOLDER_UPDATED(HttpStatus.OK, "LINKU2007", "링크 폴더 이동에 성공했습니다."),
    LINKU_QUICK_SEARCH_OK(HttpStatus.OK, "LINKU2008", "검색어 자동완성 결과를 가져왔습니다."),
    SEARCH_HISTORY_OK(HttpStatus.OK, "LINKU2009", "최근 검색어를 가져왔습니다."),
    SEARCH_HISTORY_DELETED(HttpStatus.OK, "LINKU2010", "검색어를 삭제했습니다."),
    SEARCH_HISTORY_ALL_DELETED(HttpStatus.OK, "LINKU2011", "검색어를 모두 삭제했습니다.");
    LINKU_DELETED(HttpStatus.OK, "LINKU2006", "링크 삭제에 성공했습니다."),
    LINKU_LAST_MONTH_UNREAD_OK(HttpStatus.OK, "LINKU2007", "저번 달 저장만 하고 열어보지 않은 링크를 가져왔습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public SuccessReasonDTO getReason() {
        return SuccessReasonDTO.builder().message(message).code(code).isSuccess(true).build();
    }

    @Override
    public SuccessReasonDTO getReasonHttpStatus() {
        return SuccessReasonDTO.builder().message(message).code(code).isSuccess(true).httpStatus(httpStatus).build();
    }
}
