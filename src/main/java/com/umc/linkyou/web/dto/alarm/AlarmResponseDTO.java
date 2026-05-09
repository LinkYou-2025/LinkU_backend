package com.umc.linkyou.web.dto.alarm;

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
}
