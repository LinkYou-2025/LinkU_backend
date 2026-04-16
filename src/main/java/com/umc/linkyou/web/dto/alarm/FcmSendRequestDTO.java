package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.enums.AlarmType;
import lombok.Getter;

@Getter
public class FcmSendRequestDTO {

    private final AlarmType type;
    private final Long targetId;
    private final String nickname; // nullable, CURATION 알림 시 사용

    private FcmSendRequestDTO(AlarmType type, Long targetId, String nickname) {
        this.type = type;
        this.targetId = targetId;
        this.nickname = nickname;
    }

    // 일반 알림 - title/body 모두 AlarmType에서 자동 설정
    public static FcmSendRequestDTO of(AlarmType type, Long targetId) {
        return new FcmSendRequestDTO(type, targetId, null);
    }

    // 큐레이션 알림 - body의 %s를 nickname으로 치환
    public static FcmSendRequestDTO ofWithNickname(AlarmType type, Long targetId, String nickname) {
        return new FcmSendRequestDTO(type, targetId, nickname);
    }

    public String getTitle() {
        return type.getTitle();
    }

    public String getMessage() {
        if (nickname != null) {
            return String.format(type.getBody(), nickname);
        }
        return type.getBody();
    }
}
