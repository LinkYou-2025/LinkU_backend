package com.umc.linkyou.service.alarm;

import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmSettingResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmService 단위 테스트")
class AlarmServiceTest {

    @InjectMocks private AlarmService alarmService;

    @Mock private UserFcmTokenRepository userFcmTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private AlarmSettingRepository alarmSettingRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private UserAlarmRepository userAlarmRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID = 1L;
    private static final Long ALARM_ID = 10L;
    private static final String FCM_TOKEN = "test-fcm-token";

    private Users stubUser() {
        return Users.builder().id(USER_ID).nickName("테스트유저").role(Role.USER).build();
    }

    private AlarmSetting stubDefaultSetting(Users user) {
        return AlarmSetting.createDefault(user);
    }

    @Nested
    @DisplayName("FCM 토큰 등록 (registerFcmToken)")
    class RegisterFcmToken {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("신규 토큰이면 UsersFcmToken을 저장한다")
            void 신규토큰_저장() {
                Users user = stubUser();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(null);

                alarmService.registerFcmToken(USER_ID, new AlarmRequestDTO.AlarmFcmTokenDTO(FCM_TOKEN));

                verify(userFcmTokenRepository).save(any(UsersFcmToken.class));
            }

            @Test
            @DisplayName("이미 등록된 토큰이면 activate만 호출한다")
            void 기존토큰_활성화() {
                Users user = stubUser();
                UsersFcmToken existingToken = UsersFcmToken.builder()
                        .user(user).fcmToken(FCM_TOKEN)
                        .lastUsedAt(LocalDateTime.now().minusDays(5))
                        .expiresAt(LocalDateTime.now().plusDays(55))
                        .isActive(false)
                        .build();

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(existingToken);

                alarmService.registerFcmToken(USER_ID, new AlarmRequestDTO.AlarmFcmTokenDTO(FCM_TOKEN));

                assertThat(existingToken.getIsActive()).isTrue();
                verify(userFcmTokenRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 유저면 _USER_NOT_FOUND를 던진다")
            void 유저없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        alarmService.registerFcmToken(USER_ID, new AlarmRequestDTO.AlarmFcmTokenDTO(FCM_TOKEN)))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("FCM 토큰 비활성화 (deleteFcmToken)")
    class DeleteFcmToken {

        @Test
        @DisplayName("토큰이 존재하면 deactivate 처리한다")
        void 토큰존재_비활성화() {
            Users user = stubUser();
            UsersFcmToken token = UsersFcmToken.builder()
                    .user(user).fcmToken(FCM_TOKEN)
                    .lastUsedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(60))
                    .isActive(true).build();
            given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(token);

            alarmService.deleteFcmToken(USER_ID, FCM_TOKEN);

            assertThat(token.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("토큰이 없어도 예외 없이 종료한다")
        void 토큰없음_노옵() {
            given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(null);

            alarmService.deleteFcmToken(USER_ID, FCM_TOKEN);

            verify(userFcmTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("알림 설정 조회 (viewAlarm)")
    class ViewAlarm {

        @Test
        @DisplayName("설정이 있으면 AlarmSettingResponseDTO를 반환한다")
        void 설정조회_성공() {
            Users user = stubUser();
            AlarmSetting setting = stubDefaultSetting(user);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

            AlarmSettingResponseDTO result = alarmService.viewAlarm(USER_ID);

            assertThat(result.isAllEnabled()).isTrue();
            assertThat(result.isLinkEnabled()).isTrue();
            assertThat(result.isNoticeEnabled()).isTrue();
        }

        @Test
        @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
        void 유저없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.viewAlarm(USER_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
        }

        @Test
        @DisplayName("알림 설정이 없으면 ALARM_SETTING_NOT_INITIALIZED를 던진다")
        void 설정없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(stubUser()));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.viewAlarm(USER_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_SETTING_NOT_INITIALIZED));
        }
    }

    @Nested
    @DisplayName("알림 설정 수정 (updateNoticeAlarmSetting)")
    class UpdateNoticeAlarmSetting {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("ALL 토글 시 모든 설정이 반전되고 NOTICE 이벤트가 발행된다")
            void ALL_토글_이벤트발행() {
                Users user = stubUser();
                AlarmSetting setting = stubDefaultSetting(user);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.ALL);

                assertThat(result.isAllEnabled()).isFalse();
                assertThat(result.isLinkEnabled()).isFalse();
                verify(eventPublisher).publishEvent((Object) any());
            }

            @Test
            @DisplayName("NOTICE 토글 시 notice 설정이 반전되고 이벤트가 발행된다")
            void NOTICE_토글_이벤트발행() {
                Users user = stubUser();
                AlarmSetting setting = stubDefaultSetting(user);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.NOTICE);

                assertThat(result.isNoticeEnabled()).isFalse();
                verify(eventPublisher).publishEvent((Object) any());
            }

            @Test
            @DisplayName("LINK 토글 시 link 설정만 반전되고 이벤트는 발행되지 않는다")
            void LINK_토글_이벤트없음() {
                Users user = stubUser();
                AlarmSetting setting = stubDefaultSetting(user);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK);

                assertThat(result.isLinkEnabled()).isFalse();
                assertThat(result.isAllEnabled()).isTrue();
                verify(eventPublisher, never()).publishEvent((Object) any());
            }

            @Test
            @DisplayName("개별 설정을 모두 끄면 isAllEnabled도 false가 된다")
            void 개별설정_모두끔_전체비활성화() {
                Users user = stubUser();
                AlarmSetting setting = stubDefaultSetting(user);
                setting.updateNotice(false);
                setting.updateFolder(false);
                setting.updateCuration(false);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK);

                assertThat(result.isLinkEnabled()).isFalse();
                assertThat(result.isAllEnabled()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
            void 유저없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }

            @Test
            @DisplayName("알림 설정이 없으면 ALARM_SETTING_NOT_INITIALIZED를 던진다")
            void 설정없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(stubUser()));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.NOTICE))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(AlarmErrorStatus.ALARM_SETTING_NOT_INITIALIZED));
            }
        }
    }

    @Nested
    @DisplayName("개인 알림 발송 (sendAlarm)")
    class SendAlarm {

        @Test
        @DisplayName("알림이 저장되고 PersonalAlarmEvent가 발행된다")
        void 알림발송_성공() {
            Users user = stubUser();
            Alarm savedAlarm = Alarm.create(AlarmType.LINK_SUMMARY_COMPLETE, 100L);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.save(any())).willReturn(savedAlarm);

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.LINK_SUMMARY_COMPLETE, 100L));

            verify(alarmRepository).save(any());
            verify(userAlarmRepository).save(any());
            verify(eventPublisher).publishEvent((Object) any());
        }

        @Test
        @DisplayName("CURATION_UPDATED 타입이면 body에 닉네임이 포함된다")
        void 큐레이션알림_닉네임포함() {
            Users user = stubUser();
            ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.save(alarmCaptor.capture())).willAnswer(inv -> inv.getArgument(0));

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.CURATION_UPDATED, 200L));

            assertThat(alarmCaptor.getValue().getBody()).contains("테스트유저");
        }

        @Test
        @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
        void 유저없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                            AlarmType.LINK_SUMMARY_COMPLETE, 1L)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("관리자 브로드캐스트 알림 등록 (registerAdminAlarm)")
    class RegisterAdminAlarm {

        @Test
        @DisplayName("알림이 저장되고 브로드캐스트 이벤트 2개가 발행된다")
        void 브로드캐스트_알림등록_성공() {
            Alarm savedAlarm = mock(Alarm.class);
            given(savedAlarm.getId()).willReturn(ALARM_ID);
            given(alarmRepository.save(any())).willReturn(savedAlarm);

            alarmService.registerAdminAlarm(new AlarmRequestDTO.AdminAlarmSendRequestDTO(
                    AlarmType.ANNOUNCEMENT_UPDATE, "공지 내용"));

            verify(eventPublisher, times(2)).publishEvent((Object) any());
        }

        @Test
        @DisplayName("AlarmType이 ALL이면 ALARM_TOPIC_SUBSCRIPTION_FAILED를 던진다")
        void ALL타입_예외() {
            AlarmType mockType = mock(AlarmType.class);
            given(mockType.getSettingType()).willReturn(AlarmSettingType.ALL);

            assertThatThrownBy(() ->
                    alarmService.registerAdminAlarm(new AlarmRequestDTO.AdminAlarmSendRequestDTO(
                            mockType, "내용")))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED));
        }
    }

    @Nested
    @DisplayName("알림 목록 조회 (viewAlarmList)")
    class ViewAlarmList {

        @Test
        @DisplayName("ALL 타입이면 전체 알림 목록을 반환한다")
        void 전체알림_목록조회_성공() {
            Users user = stubUser();
            Alarm alarm = Alarm.create(AlarmType.CURATION_UPDATED, 1L);
            UserAlarm userAlarm = UserAlarm.create(user, alarm);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAlarmRepository.findAlarmListByCursor(
                    eq(USER_ID), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(userAlarm));

            AlarmResponseDTO.AlarmCursorPageResponse result =
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.ALL, null, 10);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("특정 타입 필터로 조회하면 해당 타입만 반환한다")
        void 타입필터_알림목록조회_성공() {
            Users user = stubUser();
            Alarm alarm = Alarm.create(AlarmType.FOLDER_DELETED, 2L);
            UserAlarm userAlarm = UserAlarm.create(user, alarm);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAlarmRepository.findAlarmListByCursor(
                    eq(USER_ID), anyLong(), anyList(), any(Pageable.class)))
                    .willReturn(List.of(userAlarm));

            AlarmResponseDTO.AlarmCursorPageResponse result =
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.FOLDER, null, 10);

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("size가 0 이하면 빈 목록을 반환한다")
        void 사이즈0이하_빈목록반환() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(stubUser()));

            AlarmResponseDTO.AlarmCursorPageResponse result =
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.ALL, null, 0);

            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
        void 유저없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.ALL, null, 10))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("알림 상세 조회 (viewAlarmDetail)")
    class ViewAlarmDetail {

        @Test
        @DisplayName("알림이 있으면 AlarmDetailDTO를 반환한다")
        void 알림상세_조회_성공() {
            Alarm alarm = Alarm.create(AlarmType.ANNOUNCEMENT_UPDATE, 1L);
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm));

            AlarmResponseDTO.AlarmDetailDTO result = alarmService.viewAlarmDetail(USER_ID, ALARM_ID);

            assertThat(result.title()).isEqualTo(AlarmType.ANNOUNCEMENT_UPDATE.getTitle());
            assertThat(result.content()).isNotBlank();
        }

        @Test
        @DisplayName("알림이 없으면 ALARM_NOT_FOUND를 던진다")
        void 알림없음_예외() {
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.viewAlarmDetail(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리 (markAlarmAsRead)")
    class MarkAlarmAsRead {

        @Test
        @DisplayName("읽음 처리 시 isRead가 true로 변경된다")
        void 읽음처리_성공() {
            Users user = stubUser();
            Alarm alarm = Alarm.create(AlarmType.LINK_SUMMARY_COMPLETE, 1L);
            UserAlarm userAlarm = UserAlarm.create(user, alarm);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm));
            given(userAlarmRepository.findByUserAndAlarm(user, alarm)).willReturn(userAlarm);

            alarmService.markAlarmAsRead(USER_ID, ALARM_ID);

            assertThat(userAlarm.isRead()).isTrue();
        }

        @Test
        @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
        void 유저없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.markAlarmAsRead(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
        }

        @Test
        @DisplayName("알림이 없으면 ALARM_NOT_FOUND를 던진다")
        void 알림없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(stubUser()));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.markAlarmAsRead(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_NOT_FOUND));
        }

        @Test
        @DisplayName("UserAlarm이 없으면 ALARM_NOT_FOUND를 던진다")
        void UserAlarm없음_예외() {
            Users user = stubUser();
            Alarm alarm = Alarm.create(AlarmType.LINK_SUMMARY_COMPLETE, 1L);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm));
            given(userAlarmRepository.findByUserAndAlarm(user, alarm)).willReturn(null);

            assertThatThrownBy(() -> alarmService.markAlarmAsRead(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_NOT_FOUND));
        }
    }
}
