package com.umc.linkyou.service.alarm;


import com.google.firebase.messaging.*;
import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Collections.singletonList;

@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    // 앱 아이콘/색 설정
    private final String APP_ICON = "notice_icon";
    private final String COLOR_CODE = "#FF0000";
    private final String CLICK_ACTION = "notice_icon_click";

    private final FirebaseMessaging firebaseMessaging;
    private final UserFcmTokenRepository userFcmTokenRepository;

    // 사용자 단일 메세지
    @Override
    @Transactional
    public void sendFcmMessage(Long userId, FcmSendRequestDTO requestDTO) throws FirebaseMessagingException {
        // 여러 기기에 보낼 수 있으므로 리스트 처리
        List<UsersFcmToken> activeTokens = userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(userId);
        sendMessagesToTokens(activeTokens, requestDTO);
    }

    // 브로드캐스트 메시지
    @Override
    @Transactional
    public void sendTopicFcmMessage(FcmSendRequestDTO requestDTO) throws FirebaseMessagingException {
        List<UsersFcmToken> activeTokens = userFcmTokenRepository.findAllByIsActiveTrue();
        sendMessagesToTokens(activeTokens, requestDTO);
    }

    // 주제 구독
    @Override
    public void subscribeTopic(String token, String topic) throws FirebaseMessagingException {
        	firebaseMessaging.subscribeToTopic(
                    singletonList(token),
                    topic
            );
    }

    // 주제 구독 취소
    @Override
    public void unsubscribeTopic(String token, String topic) throws FirebaseMessagingException {
        firebaseMessaging.unsubscribeFromTopic(
                singletonList(token),
                topic
        );
    }

    // 토큰 전송 실패를 판단
    private boolean isPermanentTokenFailure(FirebaseMessagingException exception) {
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    // 활성화된 토큰에 메시지 전송 / 실패 시 영구적 실패는 토큰 비활성화 / 일시적 실패는 마지막 예외 저장 후 반복 종료
    private void sendMessagesToTokens(List<UsersFcmToken> activeTokens, FcmSendRequestDTO requestDTO)
            throws FirebaseMessagingException {
        FirebaseMessagingException lastTransientException = null;

        for (UsersFcmToken userFcmToken : activeTokens) {
            Message message = buildMessage(userFcmToken.getFcmToken(), requestDTO);

            try {
                firebaseMessaging.send(message);
                userFcmToken.activate();
            } catch (FirebaseMessagingException exception) {
                if (isPermanentTokenFailure(exception)) {
                    userFcmToken.deactivate();
                    continue;
                }
                lastTransientException = exception;
            }
        }

        if (lastTransientException != null) {
            throw lastTransientException;
        }
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
                                .build()).build()
                )
                .build();
    }
}
