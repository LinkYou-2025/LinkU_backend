package com.umc.linkyou.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmType {

    LINK_SUMMARY_COMPLETE(
            "링크 요약 완료",
            "'{linkTitle}' 링크에 대한 AI 요약이 완료되었어요",
            AlarmSettingType.LINK,
            AlarmResponseType.LINK
    ),

    SHARED_FOLDER_INVITE(
            "공유폴더 초대",
            "{nickname}님이 '{folderName}' 폴더를 공유했어요. 지금 바로 확인해보세요!",
            AlarmSettingType.FOLDER,
            AlarmResponseType.FOLDER
    ),

    FOLDER_DELETED(
            "폴더 삭제",
            "{nickname}님이 '{folderName}' 폴더를 삭제해 더 이상 접근할 수 없어요",
            AlarmSettingType.FOLDER,
            AlarmResponseType.FOLDER
    ),

    FOLDER_PERMISSION_CHANGED(
            "공유 폴더 권한 변경",
            "'{folderName}' 폴더의 편집 권한이 변경됐어요",
            AlarmSettingType.FOLDER,
            AlarmResponseType.FOLDER
    ),

    CURATION_UPDATED(
            "이번 달 큐레이션",
            "{nickname}님을 위한 이 달의 큐레이션이 도착했어요!",
            AlarmSettingType.CURATION,
            AlarmResponseType.CURATION
    ),

    ANNOUNCEMENT_UPDATE(
            "앱 업데이트",
            "새로운 기능이 추가됐어요, 지금 확인해보세요",
            AlarmSettingType.NOTICE,
            AlarmResponseType.NOTICE
    ),

    ANNOUNCEMENT_ERROR(
            "서비스 안내",
            "일부 기능 이용이 원활하지 않을 수 있어요",
            AlarmSettingType.NOTICE,
            AlarmResponseType.NOTICE
    );

    private final String title;
    private final String body;
    private final AlarmSettingType settingType;
    private final AlarmResponseType responseType;
}
