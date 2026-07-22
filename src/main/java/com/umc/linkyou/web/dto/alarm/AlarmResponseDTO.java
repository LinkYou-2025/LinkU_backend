package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.enums.AlarmResponseType;
import io.swagger.v3.oas.annotations.media.Schema;

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
            @Schema(description = "알림 타입")
            AlarmResponseType alarmType,
            String message,
            LocalDateTime createdAt,
            @Schema(description = "알림이 연결된 대상 ID")
            Long targetId,
            boolean isRead
    ){}

    public record AlarmCursorPageResponse(
            List<AlarmListDTO> items,
            Long nextCursor,
            boolean hasNext
    ){}

    public record UnreadAlarmExistsDTO(
            @Schema(description = "읽지 않은 알림 존재 여부")
            boolean hasUnread
    ){}
}
