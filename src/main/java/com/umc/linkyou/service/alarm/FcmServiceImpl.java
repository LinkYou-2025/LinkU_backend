package com.umc.linkyou.service.alarm;

import com.google.firebase.messaging.*;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmPushSender, FcmSubscriber {

    private static final String APP_ICON = "notice_icon";
    private static final String COLOR_CODE = "#FF0000";
    private static final String CLICK_ACTION = "notice_icon_click";

    private final FirebaseMessaging firebaseMessaging;
    private final UserFcmTokenRepository userFcmTokenRepository;

    // 사용자 전체 기기에 멀티캐스트 전송
    @Override
    @Transactional
    public void sendToUser(Long userId, FcmSendRequestDTO requestDTO) {
        List<UsersFcmToken> activeTokens = userFcmTokenRepository.findAllActiveAndNotExpiredByUserId(userId, LocalDateTime.now());
        if (activeTokens.isEmpty()) return;

        List<String> tokenStrings = activeTokens.stream()
                .map(UsersFcmToken::getFcmToken)
                .toList();

        MulticastMessage message = buildMulticastMessage(tokenStrings, requestDTO);

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            handleBatchResponse(response, activeTokens);
        } catch (FirebaseMessagingException e) {
            throw new GeneralException(AlarmErrorStatus.ALARM_SEND_FAILED);
        }
    }

    // 단일 토큰 전송 (테스트용)
    @Override
    public void sendToToken(String token, FcmSendRequestDTO requestDTO) {
        try {
            firebaseMessaging.send(buildMessage(token, requestDTO));
        } catch (FirebaseMessagingException e) {
            throw new GeneralException(AlarmErrorStatus.ALARM_SEND_FAILED);
        }
    }

    // 토픽 브로드캐스트 전송
    @Override
    public void sendToTopic(FcmSendRequestDTO requestDTO) {
        try {
            firebaseMessaging.send(buildTopicMessage(resolveTopic(requestDTO), requestDTO));
        } catch (FirebaseMessagingException e) {
            throw new GeneralException(AlarmErrorStatus.ALARM_SEND_FAILED);
        }
    }

    // 토픽 구독 상태 일괄 업데이트
    @Override
    public void updateTopicSubscription(String token, List<String> topics, boolean shouldSubscribe) {
        List<String> failedTopics = new ArrayList<>();
        for (String topic : topics) {
            try {
                if (shouldSubscribe) {
                    firebaseMessaging.subscribeToTopic(singletonList(token), topic);
                } else {
                    firebaseMessaging.unsubscribeFromTopic(singletonList(token), topic);
                }
            } catch (FirebaseMessagingException e) {
                log.warn("토픽 구독 상태 변경 실패 - topic: {}, action: {}, error: {}",
                        topic, shouldSubscribe ? "subscribe" : "unsubscribe", e.getMessage());
                failedTopics.add(topic);
            }
        }
        if (!failedTopics.isEmpty()) {
            throw new GeneralException(AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED);
        }
    }

    // 멀티캐스트 응답 처리 - 영구 실패 토큰 비활성화
    private void handleBatchResponse(BatchResponse response, List<UsersFcmToken> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                tokens.get(i).activate();
            } else {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    tokens.get(i).deactivate();
                }
            }
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, FcmSendRequestDTO requestDTO) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("title", requestDTO.getTitle())
                .putData("body", requestDTO.getMessage())
                .putData("type", requestDTO.getType().name())
                .putData("targetId", requestDTO.getTargetId().toString())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setIcon(APP_ICON)
                                .setColor(COLOR_CODE)
                                .setClickAction(CLICK_ACTION)
                                .build())
                        .build())
                .build();
    }

    private Message buildMessage(String token, FcmSendRequestDTO requestDTO) {
        return Message.builder()
                .putData("title", requestDTO.getTitle())
                .putData("body", requestDTO.getMessage())
                .putData("type", requestDTO.getType().name())
                .putData("targetId", requestDTO.getTargetId().toString())
                .setToken(token)
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setIcon(APP_ICON)
                                .setColor(COLOR_CODE)
                                .setClickAction(CLICK_ACTION)
                                .build())
                        .build())
                .build();
    }

    private Message buildTopicMessage(String topic, FcmSendRequestDTO requestDTO) {
        return Message.builder()
                .putData("title", requestDTO.getTitle())
                .putData("body", requestDTO.getMessage())
                .putData("type", requestDTO.getType().name())
                .putData("targetId", requestDTO.getTargetId().toString())
                .setTopic(topic)
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setIcon(APP_ICON)
                                .setColor(COLOR_CODE)
                                .setClickAction(CLICK_ACTION)
                                .build())
                        .build())
                .build();
    }

    private String resolveTopic(FcmSendRequestDTO requestDTO) {
        AlarmSettingType settingType = requestDTO.getType().getSettingType();
        return switch (settingType) {
            case NOTICE -> "alarm-notice";
            case LINK -> "alarm-link";
            case FOLDER -> "alarm-folder";
            case CURATION -> "alarm-curation";
            case ALL -> throw new GeneralException(AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED);
        };
    }
}
