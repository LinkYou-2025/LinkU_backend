package com.umc.linkyou.service.alarm;

import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnMissingBean({FcmPushSender.class, FcmSubscriber.class})
public class FcmServiceNoop implements FcmPushSender, FcmSubscriber {

    @Override
    public void sendToUser(Long userId, FcmSendRequestDTO requestDTO) {
        log.debug("FCM disabled — skipping sendToUser for userId={}", userId);
    }

    @Override
    public void sendToToken(String token, FcmSendRequestDTO requestDTO) {
        log.debug("FCM disabled — skipping sendToToken");
    }

    @Override
    public void sendToTopic(FcmSendRequestDTO requestDTO) {
        log.debug("FCM disabled — skipping sendToTopic");
    }

    @Override
    public void updateTopicSubscription(String token, List<String> topics, boolean shouldSubscribe) {
        log.debug("FCM disabled — skipping updateTopicSubscription");
    }
}
