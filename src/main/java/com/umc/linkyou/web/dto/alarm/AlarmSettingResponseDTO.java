package com.umc.linkyou.web.dto.alarm;


public record AlarmSettingResponseDTO(
        boolean isAllEnabled,
        boolean isLinkEnabled,
        boolean isFolderEnabled,
        boolean isCurationEnabled,
        boolean isNoticeEnabled
) {
}
