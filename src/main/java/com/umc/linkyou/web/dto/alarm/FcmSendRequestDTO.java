package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.enums.AlarmType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendRequestDTO {

    private String title;
    private String message;
    private AlarmType type;
    private Long targetId;
}
