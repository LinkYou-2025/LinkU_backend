package com.umc.linkyou.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmType {

    LINK_SUMMARY_COMPLETE(
            "링크 요약 완료",
            "저장한 링크 요약이 준비됐어요"
    ),

    FOLDER_DELETED(
            "폴더 삭제",
            "공유받은 폴더에 더 이상 접근할 수 없어요"
    ),

    FOLDER_PERMISSION_CHANGED(
            "공유폴더 권한 변경",
            "공유 폴더의 편집 권한이 변경됐어요"
    ),

    CURATION_UPDATED(
            "이번 달 큐레이션",
            "%s님을 위한 이 달의 큐레이션이 도착했어요!"
    ),

    ANNOUNCEMENT_UPDATE(
            "앱 업데이트",
            "새로운 기능이 추가됐어요, 지금 확인해보세요"
    ),

    ANNOUNCEMENT_ERROR(
            "서비스 안내",
            "일부 기능 이용이 원활하지 않을 수 있어요"
    );

    private final String title;
    private final String body;
}