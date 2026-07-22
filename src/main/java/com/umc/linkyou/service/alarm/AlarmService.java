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
import com.umc.linkyou.service.alarm.event.BroadCastAlarmEvent;
import com.umc.linkyou.service.alarm.event.BroadCastUserAlarmCreateEvent;
import com.umc.linkyou.service.alarm.event.PersonalAlarmEvent;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmSettingResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {
    private static final String NOTICE_TOPIC = "alarm-notice";

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;
    private final AlarmSettingRepository alarmSettingRepository;
    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final ApplicationEventPublisher eventPublisher;

    // FCM 토큰 등록
    @Transactional
    public void registerFcmToken(Long userId, AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        String newToken = alarmFcmTokenDTO.fcmToken();
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, newToken);

        if (existingToken == null) {
            userFcmTokenRepository.save(UsersFcmToken.builder()
                    .user(user)
                    .fcmToken(newToken)
                    .lastUsedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(60))
                    .isActive(true)
                    .build());
        } else {
            existingToken.activate();
        }

        alarmSettingRepository.findByUserId(userId)
                .filter(alarmSetting -> alarmSetting.isEnabled(AlarmSettingType.NOTICE))
                .ifPresent(alarmSetting -> eventPublisher.publishEvent(
                        new AlarmSettingChangedEvent(userId, AlarmSettingType.NOTICE, true, List.of(NOTICE_TOPIC))
                ));
    }

    // FCM 토큰 비활성화
    @Transactional
    public void deleteFcmToken(Long userId, String fcmToken) {
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, fcmToken);
        if (existingToken != null) {
            existingToken.deactivate();
        }
    }

    // 알림 설정 조회
    public AlarmSettingResponseDTO viewAlarm(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_SETTING_NOT_INITIALIZED));
        return new AlarmSettingResponseDTO(
                alarmSetting.isAlarmAllEnabled(),
                alarmSetting.isLinkActive(),
                alarmSetting.isFolderActive(),
                alarmSetting.isCurationActive(),
                alarmSetting.isNoticeActive()
        );
    }

    // 알림 설정 수정
    @Transactional
    public AlarmSettingResponseDTO updateNoticeAlarmSetting(Long userId, AlarmSettingType alarmSettingType) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_SETTING_NOT_INITIALIZED));

        // 알림 타입에 따른 분기
        switch (alarmSettingType) {
            case ALL -> alarmSetting.updateAll(!alarmSetting.isAlarmAllEnabled());
            case NOTICE -> alarmSetting.updateNotice(!alarmSetting.isNoticeEnabled());
            case LINK -> alarmSetting.updateLink(!alarmSetting.isLinkEnabled());
            case CURATION -> alarmSetting.updateCuration(!alarmSetting.isCurationEnabled());
            case FOLDER -> alarmSetting.updateFolder(!alarmSetting.isFolderEnabled());
        }

        // 전체 알림이 아니라면, 전체 알림도 같이 동기화
        if (alarmSettingType != AlarmSettingType.ALL) {
            boolean anyRawEnabled = alarmSetting.isNoticeEnabled() || alarmSetting.isLinkEnabled()
                    || alarmSetting.isCurationEnabled() || alarmSetting.isFolderEnabled();
            if (!anyRawEnabled) {
                alarmSetting.updateAlarmAllEnabled(false);
            }
        }

        alarmSettingRepository.save(alarmSetting);

        // 토픽 브로드캐스트는 NOTICE 계열만 사용한다. LINK/FOLDER/CURATION은 개인 토큰 push 대상이다.
        if (alarmSettingType == AlarmSettingType.NOTICE || alarmSettingType == AlarmSettingType.ALL) {
            boolean shouldSubscribe = alarmSetting.isEnabled(AlarmSettingType.NOTICE);
            eventPublisher.publishEvent(
                    new AlarmSettingChangedEvent(userId, alarmSettingType, shouldSubscribe, List.of(NOTICE_TOPIC))
            );
        }

        return new AlarmSettingResponseDTO(
                alarmSetting.isAlarmAllEnabled(),
                alarmSetting.isLinkActive(),
                alarmSetting.isFolderActive(),
                alarmSetting.isCurationActive(),
                alarmSetting.isNoticeActive()
        );
    }

    // 개인 알림 전송
    // userId, 알림타입, targetId를 설정하면 메세지 내용을 자동으로 설정합니다.
    // 알림 설정 검사는 여기서 중앙 처리한다: alarmType.getSettingType() 설정이
    // 꺼졌거나 초기화되지 않은 유저면 발송을 스킵한다. (호출자는 별도 검사 불필요)
    @Transactional
    public void sendAlarm(Long userId, AlarmRequestDTO.AlarmSendRequestDTO requestDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmType alarmType = requestDTO.type();

        // 해당 알림 종류의 설정이 켜진 유저에게만 발송한다.
        boolean enabled = alarmSettingRepository.findByUserId(userId)
                .map(setting -> setting.isEnabled(alarmType.getSettingType()))
                .orElse(false);
        if (!enabled) {
            return;
        }

        // CURATION 은 서버가 nickname 을 채우고, 그 외에는 요청 payload 를 사용한다.
        AlarmPayload payload = alarmType == AlarmType.CURATION_UPDATED
                ? new AlarmPayload.Nickname(user.getNickName())
                : (requestDTO.payload() != null ? requestDTO.payload() : new AlarmPayload.Empty());

        String renderedBody = AlarmMessageRenderer.render(alarmType.getBody(), payload.toValues());

        Alarm alarm = alarmRepository.save(Alarm.create(alarmType, requestDTO.targetId(), renderedBody));
        userAlarmRepository.save(UserAlarm.create(user, alarm));

        eventPublisher.publishEvent(
                PersonalAlarmEvent.withPayload(userId, alarmType, requestDTO.targetId(), alarm.getId(), payload));
    }

    // 다수 유저에게 동일 본문으로 일괄 발송
    // 설정 검사는 여기서 중앙 처리하고, Alarm 1건 + UserAlarm N건을 배치 저장
    // payload 는 모든 수신자에게 동일해야 함
    @Transactional
    public void sendAlarmBulk(List<Long> userIds, AlarmType alarmType, Long targetId, AlarmPayload payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // 설정이 켜진 유저만 추림
        List<Long> enabledUserIds = alarmSettingRepository.findAllByUserIdIn(userIds).stream()
                .filter(setting -> setting.isEnabled(alarmType.getSettingType()))
                .map(setting -> setting.getUser().getId())
                .toList();
        if (enabledUserIds.isEmpty()) {
            return;
        }

        String renderedBody = AlarmMessageRenderer.render(alarmType.getBody(), payload.toValues());
        Alarm alarm = alarmRepository.save(Alarm.create(alarmType, targetId, renderedBody));

        List<UserAlarm> userAlarms = enabledUserIds.stream()
                .map(uid -> UserAlarm.create(userRepository.getReferenceById(uid), alarm))
                .toList();
        userAlarmRepository.saveAll(userAlarms);

        enabledUserIds.forEach(uid ->
                eventPublisher.publishEvent(
                        PersonalAlarmEvent.withPayload(uid, alarmType, targetId, alarm.getId(), payload)));
    }

    // 관리자 브로드캐스트 알림 등록, content 직접 입력
    @Transactional
    public void registerAdminAlarm(AlarmRequestDTO.AdminAlarmSendRequestDTO requestDTO) {
        AlarmType alarmType = requestDTO.type();

        // targetId는 임시로 설정
        Alarm alarm = alarmRepository.save(Alarm.create(alarmType, 0L, requestDTO.content()));
        // 알림 생성 후에 업데이트
        alarm.updateTargetId(alarm.getId());

        eventPublisher.publishEvent(new BroadCastAlarmEvent(alarmType, alarm.getId()));
        eventPublisher.publishEvent(new BroadCastUserAlarmCreateEvent(alarm.getId()));
    }

    // 알림 목록 조회 - 타입별 필터, 커서 페이징
    public AlarmResponseDTO.AlarmCursorPageResponse viewAlarmList(
            Long userId,
            AlarmSettingType alarmSettingType,
            Long cursor,
            int size
    ) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (size <= 0) {
            return new AlarmResponseDTO.AlarmCursorPageResponse(List.of(), null, false);
        }

        Pageable pageable = PageRequest.of(0, size + 1);
        Long safeCursor = (cursor == null) ? Long.MAX_VALUE : cursor;

        List<UserAlarm> fetched;


        if (alarmSettingType == AlarmSettingType.ALL) {
            fetched = userAlarmRepository.findAlarmListByCursor(userId, safeCursor, pageable);
        } else {
            // alarm 타입에 따라 분기
            List<AlarmType> alarmTypes = Arrays.stream(AlarmType.values())
                    .filter(type -> type.getSettingType() == alarmSettingType)
                    .toList();

            if (alarmTypes.isEmpty()) {
                return new AlarmResponseDTO.AlarmCursorPageResponse(List.of(), null, false);
            }

            fetched = userAlarmRepository.findAlarmListByCursor(
                    userId,
                    safeCursor,
                    alarmTypes,
                    pageable
            );
        }

        boolean hasNext = fetched.size() > size;
        List<UserAlarm> pageItems = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1).getId();

        List<AlarmResponseDTO.AlarmListDTO> alarmList = pageItems.stream()
                .map(ua -> new AlarmResponseDTO.AlarmListDTO(
                        ua.getAlarm().getId(),
                        ua.getAlarm().getAlarmType().getResponseType(),
                        ua.getAlarm().getBody(),
                        ua.getCreatedAt(),
                        ua.getAlarm().getTargetId(),
                        ua.isRead()
                ))
                .toList();

        return new AlarmResponseDTO.AlarmCursorPageResponse(alarmList, nextCursor, hasNext);
    }

    // 공지 알림 상세 조회
    public AlarmResponseDTO.AlarmDetailDTO viewAlarmDetail(Long userId, Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));
        return new AlarmResponseDTO.AlarmDetailDTO(
                alarm.getTitle(),
                alarm.getBody(),
                alarm.getCreatedAt()
        );
    }

    // 읽지 않은 알림 존재 여부 조회 (최근 30일 이내 생성된 알림만 대상)
    public AlarmResponseDTO.UnreadAlarmExistsDTO hasUnreadAlarm(Long userId) {
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(30);
        boolean hasUnread = userAlarmRepository.existsByUser_IdAndIsReadFalseAndCreatedAtAfter(userId, tenDaysAgo);
        return new AlarmResponseDTO.UnreadAlarmExistsDTO(hasUnread);
    }

    // 알림 읽음 처리
    @Transactional
    public void markAlarmAsRead(Long userId, Long alarmId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));

        UserAlarm userAlarm = userAlarmRepository.findByUserAndAlarm(user,alarm);
        if (userAlarm == null) {
            throw new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND);
        }
        userAlarm.markAsRead();
    }

}
