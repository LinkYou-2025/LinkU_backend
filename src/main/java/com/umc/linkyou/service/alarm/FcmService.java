package com.umc.linkyou.service.alarm;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;

public interface FcmService {

    void sendFcmMessage(Long userId, FcmSendRequestDTO requestDTO) throws FirebaseMessagingException;

    void sendTopicFcmMessage(FcmSendRequestDTO requestDTO) throws FirebaseMessagingException;

    void subscribeTopic(String token, String topic) throws FirebaseMessagingException;

    void unsubscribeTopic(String token, String topic) throws FirebaseMessagingException;
}
