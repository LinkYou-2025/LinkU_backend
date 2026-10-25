package com.umc.linkyou.service.alarm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.web.dto.alarm.FcmBulkTarget;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmServiceImpl 단위 테스트")
class FcmServiceImplTest {

    @Mock private FirebaseMessaging firebaseMessaging;
    @Mock private UserFcmTokenRepository userFcmTokenRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    private FcmServiceImpl fcmServiceImpl;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        fcmServiceImpl = new FcmServiceImpl(firebaseMessaging, userFcmTokenRepository, transactionManager);
    }

    private FcmBulkTarget target(Long userId, String nickname) {
        return new FcmBulkTarget(
                userId,
                FcmSendRequestDTO.withValues(
                        AlarmType.CURATION_UPDATED, 100L, 1000L, Map.of("nickname", nickname)));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("단일 토큰 전송 시 alarmId를 data에 포함한다")
    void 단일토큰_전송_alarmId포함() throws Exception {
        fcmServiceImpl.sendToToken(
                "test-token",
                FcmSendRequestDTO.of(AlarmType.ANNOUNCEMENT_UPDATE, 100L, 1000L));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(captor.capture());
        Map<String, String> data = (Map<String, String>) ReflectionTestUtils.getField(captor.getValue(), "data");
        assertThat(data)
                .containsEntry("alarmId", "1000")
                .containsEntry("targetId", "100");
    }

    @Nested
    @DisplayName("개인화 알림 일괄 발송 (sendBulkPersonalized)")
    class SendBulkPersonalized {

        @Test
        @DisplayName("유저별 활성 토큰을 모아 sendEach를 한 번만 호출한다")
        void 활성토큰_모아_한번에_호출() throws Exception {
            Users userA = Users.builder().id(USER_A).nickName("에이").build();
            Users userB = Users.builder().id(USER_B).nickName("비").build();
            UsersFcmToken tokenA = UsersFcmToken.builder().user(userA).fcmToken("token-a").build();
            UsersFcmToken tokenB = UsersFcmToken.builder().user(userB).fcmToken("token-b").build();

            given(userFcmTokenRepository.findAllActiveAndNotExpiredByUserIdIn(anyList(), any()))
                    .willReturn(List.of(tokenA, tokenB));

            SendResponse success = mock(SendResponse.class);
            given(success.isSuccessful()).willReturn(true);
            BatchResponse response = mock(BatchResponse.class);
            given(response.getResponses()).willReturn(List.of(success, success));
            given(firebaseMessaging.sendEach(anyList())).willReturn(response);

            fcmServiceImpl.sendBulkPersonalized(List.of(target(USER_A, "에이"), target(USER_B, "비")));

            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(firebaseMessaging, times(1)).sendEach(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("응답이 실패면 토큰을 비활성화한다")
        void 전송실패_토큰_비활성화() throws Exception {
            Users userA = Users.builder().id(USER_A).nickName("에이").build();
            UsersFcmToken tokenA = UsersFcmToken.builder().user(userA).fcmToken("token-a").build();

            given(userFcmTokenRepository.findAllActiveAndNotExpiredByUserIdIn(anyList(), any()))
                    .willReturn(List.of(tokenA));

            SendResponse failure = mock(SendResponse.class);
            given(failure.isSuccessful()).willReturn(false);
            com.google.firebase.messaging.FirebaseMessagingException ex =
                    mock(com.google.firebase.messaging.FirebaseMessagingException.class);
            given(ex.getMessagingErrorCode())
                    .willReturn(com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED);
            given(failure.getException()).willReturn(ex);

            BatchResponse response = mock(BatchResponse.class);
            given(response.getResponses()).willReturn(List.of(failure));
            given(firebaseMessaging.sendEach(anyList())).willReturn(response);

            fcmServiceImpl.sendBulkPersonalized(List.of(target(USER_A, "에이")));

            assertThat(tokenA.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("발송 대상에게 활성 토큰이 없으면 FCM을 호출하지 않는다")
        void 활성토큰_없음_호출없음() {
            given(userFcmTokenRepository.findAllActiveAndNotExpiredByUserIdIn(anyList(), any()))
                    .willReturn(List.of());

            fcmServiceImpl.sendBulkPersonalized(List.of(target(USER_A, "에이")));

            verifyNoInteractions(firebaseMessaging);
        }

        @Test
        @DisplayName("targets가 비어있으면 토큰 조회도 하지 않는다")
        void 빈리스트_스킵() {
            fcmServiceImpl.sendBulkPersonalized(List.of());

            verifyNoInteractions(userFcmTokenRepository, firebaseMessaging);
        }
    }
}
