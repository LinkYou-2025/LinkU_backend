package com.umc.linkyou.service.alarm;

import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.event.AlarmSettingChangedEvent;
import com.umc.linkyou.support.fixture.AlarmFixture;
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

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.AlarmFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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

    @Nested
    @DisplayName("FCM 토큰 등록 (registerFcmToken)")
    class RegisterFcmToken {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("신규 토큰이면 UsersFcmToken을 저장한다")
            void 신규토큰_저장() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(null);
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

                alarmService.registerFcmToken(USER_ID, new AlarmRequestDTO.AlarmFcmTokenDTO(FCM_TOKEN));

                verify(userFcmTokenRepository).save(any(UsersFcmToken.class));
                ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                AlarmSettingChangedEvent event = (AlarmSettingChangedEvent) eventCaptor.getValue();
                assertThat(event.shouldSubscribe()).isTrue();
                assertThat(event.topics()).containsExactly("alarm-notice");
            }

            @Test
            @DisplayName("이미 등록된 토큰이면 activate 후 토픽 구독 이벤트를 발행한다")
            void 기존토큰_활성화() {
                Users user = user();
                UsersFcmToken token = AlarmFixture.inactiveToken(user);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(userFcmTokenRepository.findByUser_IdAndFcmToken(USER_ID, FCM_TOKEN)).willReturn(token);
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

                alarmService.registerFcmToken(USER_ID, new AlarmRequestDTO.AlarmFcmTokenDTO(FCM_TOKEN));

                assertThat(token.getIsActive()).isTrue();
                verify(userFcmTokenRepository, never()).save(any());
                verify(eventPublisher).publishEvent((Object) any());
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
            UsersFcmToken token = AlarmFixture.activeToken(user());
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
            Users user = user();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

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
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
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
            @DisplayName("ALL 토글 시 모든 설정이 반전되고 이벤트가 발행된다")
            void ALL_토글_이벤트발행() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.ALL);

                assertThat(result.isAllEnabled()).isFalse();
                assertThat(result.isLinkEnabled()).isFalse();
                verify(eventPublisher).publishEvent((Object) any());
            }

            @Test
            @DisplayName("NOTICE 토글 시 notice 설정이 반전되고 이벤트가 발행된다")
            void NOTICE_토글_이벤트발행() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.NOTICE);

                assertThat(result.isNoticeEnabled()).isFalse();
                verify(eventPublisher).publishEvent((Object) any());
            }

            @Test
            @DisplayName("LINK 토글 시 link 설정만 반전되고 이벤트는 발행되지 않는다")
            void LINK_토글_이벤트없음() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user)));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK);

                assertThat(result.isLinkEnabled()).isFalse();
                assertThat(result.isAllEnabled()).isTrue();
                verify(eventPublisher, never()).publishEvent((Object) any());
            }

            @Test
            @DisplayName("개별 설정 4개 모두 off 시 isAllEnabled도 false가 된다")
            void 개별설정_모두끔_마스터비활성화() {
                Users user = user();
                AlarmSetting setting = defaultSetting(user);
                setting.updateNotice(false);
                setting.updateFolder(false);
                setting.updateCuration(false);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK);

                assertThat(result.isAllEnabled()).isFalse();
                assertThat(result.isLinkEnabled()).isFalse();
                assertThat(result.isNoticeEnabled()).isFalse();
                assertThat(result.isFolderEnabled()).isFalse();
                assertThat(result.isCurationEnabled()).isFalse();
            }

            @Test
            @DisplayName("전체 off 상태에서 개별 설정을 켜도 isAllEnabled는 false 유지된다")
            void 전체off후_개별켜도_마스터유지() {
                Users user = user();
                AlarmSetting setting = defaultSetting(user);
                setting.updateAll(false);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

                AlarmSettingResponseDTO result = alarmService.updateNoticeAlarmSetting(USER_ID, AlarmSettingType.LINK);

                assertThat(result.isAllEnabled()).isFalse();
                assertThat(result.isLinkEnabled()).isFalse(); // alarmAllEnabled && linkEnabled = false
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
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
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
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user())));
            given(alarmRepository.save(any())).willReturn(alarm(AlarmType.LINK_SUMMARY_COMPLETE));

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.LINK_SUMMARY_COMPLETE, 100L, null));

            verify(alarmRepository).save(any());
            verify(userAlarmRepository).save(any());
            verify(eventPublisher).publishEvent((Object) any());
        }

        @Test
        @DisplayName("FOLDER_DELETED 타입이면 body에 닉네임과 폴더명이 정확히 치환된다")
        void 폴더삭제알림_본문전체일치() {
            ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user())));
            given(alarmRepository.save(alarmCaptor.capture())).willAnswer(inv -> inv.getArgument(0));

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.FOLDER_DELETED, 300L,
                    new AlarmPayload.NicknameAndFolder("주인", "어학")));

            assertThat(alarmCaptor.getValue().getBody())
                    .isEqualTo("주인님이 '어학' 폴더를 삭제해 더 이상 접근할 수 없어요");
        }

        @Test
        @DisplayName("CURATION_UPDATED 타입이면 body에 닉네임이 포함된다")
        void 큐레이션알림_닉네임포함() {
            ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(defaultSetting(user())));
            given(alarmRepository.save(alarmCaptor.capture())).willAnswer(inv -> inv.getArgument(0));

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.CURATION_UPDATED, 200L, null));

            assertThat(alarmCaptor.getValue().getBody())
                    .isEqualTo("테스트유저님을 위한 이 달의 큐레이션이 도착했어요!");
        }

        @Test
        @DisplayName("유저가 없으면 _USER_NOT_FOUND를 던진다")
        void 유저없음_예외() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                            AlarmType.LINK_SUMMARY_COMPLETE, 1L, null)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
        }

        @Test
        @DisplayName("해당 타입의 알림 설정이 꺼져 있으면 발송하지 않는다")
        void 설정꺼짐_발송스킵() {
            Users user = user();
            AlarmSetting setting = defaultSetting(user);
            setting.updateLink(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.LINK_SUMMARY_COMPLETE, 1L, new AlarmPayload.LinkTitle("제목")));

            verify(alarmRepository, never()).save(any());
            verify(userAlarmRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent((Object) any());
        }

        @Test
        @DisplayName("알림 설정이 초기화되지 않은 유저면 발송하지 않는다")
        void 설정미초기화_발송스킵() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            alarmService.sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.LINK_SUMMARY_COMPLETE, 1L, new AlarmPayload.LinkTitle("제목")));

            verify(alarmRepository, never()).save(any());
            verify(userAlarmRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent((Object) any());
        }
    }

    @Nested
    @DisplayName("일괄 알림 발송 (sendAlarmBulk)")
    class SendAlarmBulk {

        private static final Long MEMBER_A = 2L;
        private static final Long MEMBER_B = 3L;
        private static final Long MEMBER_C = 4L;
        private static final Long TARGET_ID = 300L;

        private final AlarmPayload payload = new AlarmPayload.NicknameAndFolder("주인", "어학");

        private Users userWith(Long id) {
            return Users.builder().id(id).nickName("u" + id).build();
        }

        @Test
        @DisplayName("설정 켜진 유저에게만 Alarm 1건 + UserAlarm N건을 배치 저장하고 이벤트를 발행한다")
        void 설정켜진유저만_배치발송() {
            AlarmSetting sA = defaultSetting(userWith(MEMBER_A));
            AlarmSetting sB = defaultSetting(userWith(MEMBER_B));
            sB.updateFolder(false); // OFF
            AlarmSetting sC = defaultSetting(userWith(MEMBER_C));

            List<Long> ids = List.of(MEMBER_A, MEMBER_B, MEMBER_C);
            given(alarmSettingRepository.findAllByUserIdIn(ids)).willReturn(List.of(sA, sB, sC));
            given(alarmRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(userRepository.getReferenceById(anyLong())).willAnswer(inv -> userWith(inv.getArgument(0)));

            alarmService.sendAlarmBulk(ids, AlarmType.FOLDER_DELETED, TARGET_ID, payload);

            verify(alarmRepository, times(1)).save(any());
            ArgumentCaptor<List<UserAlarm>> captor = ArgumentCaptor.forClass(List.class);
            verify(userAlarmRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2); // A, C 만
            verify(eventPublisher, times(2)).publishEvent((Object) any());
        }

        @Test
        @DisplayName("전원 설정이 꺼져 있으면 아무것도 저장·발행하지 않는다")
        void 전원꺼짐_스킵() {
            AlarmSetting sA = defaultSetting(userWith(MEMBER_A));
            sA.updateFolder(false);
            given(alarmSettingRepository.findAllByUserIdIn(List.of(MEMBER_A))).willReturn(List.of(sA));

            alarmService.sendAlarmBulk(List.of(MEMBER_A), AlarmType.FOLDER_DELETED, TARGET_ID, payload);

            verify(alarmRepository, never()).save(any());
            verify(userAlarmRepository, never()).saveAll(any());
            verify(eventPublisher, never()).publishEvent((Object) any());
        }

        @Test
        @DisplayName("userIds가 비어 있으면 설정 조회도 하지 않는다")
        void 빈리스트_스킵() {
            alarmService.sendAlarmBulk(List.of(), AlarmType.FOLDER_DELETED, TARGET_ID, payload);

            verifyNoInteractions(alarmSettingRepository, alarmRepository, userAlarmRepository, eventPublisher);
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
        @DisplayName("NOTICE 타입으로도 알림이 저장되고 브로드캐스트 이벤트 2개가 발행된다")
        void NOTICE_타입_브로드캐스트_알림등록_성공() {
            Alarm savedAlarm = mock(Alarm.class);
            given(savedAlarm.getId()).willReturn(ALARM_ID);
            given(alarmRepository.save(any())).willReturn(savedAlarm);

            alarmService.registerAdminAlarm(new AlarmRequestDTO.AdminAlarmSendRequestDTO(
                    AlarmType.NOTICE, "공지 내용"));

            verify(eventPublisher, times(2)).publishEvent((Object) any());
        }
    }

    @Nested
    @DisplayName("알림 목록 조회 (viewAlarmList)")
    class ViewAlarmList {

        @Test
        @DisplayName("ALL 타입이면 전체 알림 목록을 반환한다")
        void 전체알림_목록조회_성공() {
            Users user = user();
            UserAlarm ua = userAlarm(user, alarm(AlarmType.CURATION_UPDATED));

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAlarmRepository.findAlarmListByCursor(eq(USER_ID), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(ua));

            AlarmResponseDTO.AlarmCursorPageResponse result =
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.ALL, null, 10);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("특정 타입 필터로 조회하면 해당 타입만 반환한다")
        void 타입필터_알림목록조회_성공() {
            Users user = user();
            UserAlarm ua = userAlarm(user, alarm(AlarmType.FOLDER_DELETED));

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAlarmRepository.findAlarmListByCursor(eq(USER_ID), anyLong(), anyList(), any(Pageable.class)))
                    .willReturn(List.of(ua));

            AlarmResponseDTO.AlarmCursorPageResponse result =
                    alarmService.viewAlarmList(USER_ID, AlarmSettingType.FOLDER, null, 10);

            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("size가 0 이하면 빈 목록을 반환한다")
        void 사이즈0이하_빈목록반환() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));

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
    @DisplayName("읽지 않은 알림 존재 여부 조회 (hasUnreadAlarm)")
    class HasUnreadAlarm {

        @Test
        @DisplayName("읽지 않은 알림이 있으면 hasUnread=true를 반환한다")
        void 읽지않은알림_있음() {
            given(userAlarmRepository.existsByUser_IdAndIsReadFalseAndCreatedAtAfter(eq(USER_ID), any()))
                    .willReturn(true);

            AlarmResponseDTO.UnreadAlarmExistsDTO result = alarmService.hasUnreadAlarm(USER_ID);

            assertThat(result.hasUnread()).isTrue();
        }

        @Test
        @DisplayName("읽지 않은 알림이 없으면 hasUnread=false를 반환한다")
        void 읽지않은알림_없음() {
            given(userAlarmRepository.existsByUser_IdAndIsReadFalseAndCreatedAtAfter(eq(USER_ID), any()))
                    .willReturn(false);

            AlarmResponseDTO.UnreadAlarmExistsDTO result = alarmService.hasUnreadAlarm(USER_ID);

            assertThat(result.hasUnread()).isFalse();
        }
    }

    @Nested
    @DisplayName("알림 상세 조회 (viewAlarmDetail)")
    class ViewAlarmDetail {

        @Test
        @DisplayName("알림이 있으면 AlarmDetailDTO를 반환한다")
        void 알림상세_조회_성공() {
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm(AlarmType.ANNOUNCEMENT_UPDATE)));

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
            Users user = user();
            Alarm al = alarm(AlarmType.LINK_SUMMARY_COMPLETE);
            UserAlarm ua = userAlarm(user, al);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(al));
            given(userAlarmRepository.findByUserAndAlarm(user, al)).willReturn(ua);

            alarmService.markAlarmAsRead(USER_ID, ALARM_ID);

            assertThat(ua.isRead()).isTrue();
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
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> alarmService.markAlarmAsRead(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_NOT_FOUND));
        }

        @Test
        @DisplayName("UserAlarm이 없으면 ALARM_NOT_FOUND를 던진다")
        void UserAlarm없음_예외() {
            Users user = user();
            Alarm al = alarm(AlarmType.LINK_SUMMARY_COMPLETE);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(al));
            given(userAlarmRepository.findByUserAndAlarm(user, al)).willReturn(null);

            assertThatThrownBy(() -> alarmService.markAlarmAsRead(USER_ID, ALARM_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(AlarmErrorStatus.ALARM_NOT_FOUND));
        }
    }
}
