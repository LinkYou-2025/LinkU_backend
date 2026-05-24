package com.umc.linkyou.service.alarm;

import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;

public interface FcmPushSender {

    void sendToUser(Long userId, FcmSendRequestDTO requestDTO);

    void sendToToken(String token, FcmSendRequestDTO requestDTO);

    void sendToTopic(FcmSendRequestDTO requestDTO);
}