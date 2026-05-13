package com.umc.linkyou.apiPayload.code.status.folder;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FolderSuccessStatus implements BaseSuccessCode {

    // 폴더 CRUD
    FOLDER_OK(HttpStatus.OK, "FOLDER2001", "내 폴더 트리 조회에 성공했습니다."),
    FOLDER_PARENT_OK(HttpStatus.OK, "FOLDER2002", "내 중분류 폴더 조회에 성공했습니다."),
    FOLDER_SUBFOLDER_OK(HttpStatus.OK, "FOLDER2003", "하위 폴더(소분류) 조회에 성공했습니다."),
    FOLDER_CREATED(HttpStatus.CREATED, "FOLDER2011", "소분류 폴더 생성에 성공했습니다."),
    FOLDER_UPDATED(HttpStatus.OK, "FOLDER2004", "소분류 폴더명 수정에 성공했습니다."),
    FOLDER_DELETED(HttpStatus.OK, "FOLDER2005", "소분류 폴더 삭제에 성공했습니다."),
    FOLDER_LINK_OK(HttpStatus.OK, "FOLDER2006", "폴더 내부 소분류 폴더+링크 목록 조회에 성공했습니다."),
    FOLDER_BOOKMARK_OK(HttpStatus.OK, "FOLDER2007", "폴더 북마크 상태 변경에 성공했습니다."),

    // 폴더 공유
    FOLDER_INVITE_LINK_CREATED(HttpStatus.OK, "FOLDER2008", "초대 링크가 생성되었습니다."),
    FOLDER_INVITE_LINK_DEACTIVATED(HttpStatus.OK, "FOLDER2009", "초대 링크가 비활성화되었습니다."),
    FOLDER_MEMBERS_OK(HttpStatus.OK, "FOLDER2010", "폴더 참여자 목록 조회에 성공했습니다."),
    FOLDER_PERMISSION_OK(HttpStatus.OK, "FOLDER2012", "폴더 권한 수정을 성공했습니다."),
    FOLDER_UNSHARE_OK(HttpStatus.OK, "FOLDER2013", "폴더를 비공개로 전환했습니다."),
    FOLDER_SHARED_OK(HttpStatus.OK, "FOLDER2014", "공유 받은 폴더 목록 조회에 성공했습니다."),
    FOLDER_LEAVE_OK(HttpStatus.OK, "FOLDER2015", "공유 폴더에서 탈퇴했습니다."),

    // 초대
    INVITATION_INFO_OK(HttpStatus.OK, "FOLDER2016", "초대장 정보 조회에 성공했습니다."),
    INVITATION_ACCEPTED(HttpStatus.OK, "FOLDER2017", "초대를 수락했습니다.");

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
