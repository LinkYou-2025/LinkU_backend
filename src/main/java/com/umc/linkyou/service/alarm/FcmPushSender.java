package com.umc.linkyou.service.alarm;

import com.umc.linkyou.web.dto.alarm.FcmBulkTarget;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;

import java.util.List;

public interface FcmPushSender {

    void sendToUser(Long userId, FcmSendRequestDTO requestDTO);

    void sendToToken(String token, FcmSendRequestDTO requestDTO);

    void sendToTopic(FcmSendRequestDTO requestDTO);

    // 유저별로 본문이 다른 개인화 알림을 한 번의 FCM 배치 호출로 전송
    void sendBulkPersonalized(List<FcmBulkTarget> targets);
}