package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.enums.AlarmSettingType;

import java.time.LocalDateTime;
import java.util.List;

public record AlarmResponseDTO(
) {

    public record AlarmDetailDTO(
            String title,
            String content,
            LocalDateTime createdAt
    ){}

    public record AlarmListDTO(
            Long alarmId,
            AlarmSettingType alarmType,
            String message,
            LocalDateTime createdAt,
            boolean isRead
    ){}

    public record AlarmCursorPageResponse(
            List<AlarmListDTO> items,
            Long nextCursor,
            boolean hasNext
    ){}
}
