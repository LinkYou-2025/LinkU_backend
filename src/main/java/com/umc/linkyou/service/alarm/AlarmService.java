package com.umc.linkyou.service.alarm;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.redis.UserFcmTokenCache;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.repository.redis.FcmTokenRedisRepository;
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
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;
    private final AlarmSettingRepository alarmSettingRepository;
    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final FcmTokenRedisRepository fcmTokenRedisRepository;
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

        addTokenToRedis(userId, newToken);
    }

    // redis에 토큰 저장
    private void addTokenToRedis(Long userId, String token) {
        UserFcmTokenCache cache = fcmTokenRedisRepository.findById(userId)
                .orElseGet(() -> UserFcmTokenCache.builder().userId(userId).build());
        cache.addToken(token);
        fcmTokenRedisRepository.save(cache);
    }

    // redis에서 토큰 삭제
    private void removeTokenFromRedis(Long userId, String token) {
        fcmTokenRedisRepository.findById(userId).ifPresent(cache -> {
            cache.removeToken(token);
            fcmTokenRedisRepository.save(cache);
        });
    }

    // FCM 토큰 비활성화
    @Transactional
    public void deleteFcmToken(Long userId, String fcmToken) {
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, fcmToken);
        if (existingToken != null) {
            existingToken.deactivate();
        }
        removeTokenFromRedis(userId, fcmToken);
    }

    // 알림 설정 조회
    public AlarmSettingResponseDTO viewAlarm(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));
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
    public boolean updateNoticeAlarmSetting(Long userId, AlarmSettingType alarmSettingType) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));

        switch (alarmSettingType) {
            case ALL -> alarmSetting.updateAll(!alarmSetting.isAlarmAllEnabled());
            case NOTICE -> alarmSetting.updateNotice(!alarmSetting.isNoticeEnabled());
            case LINK -> alarmSetting.updateLink(!alarmSetting.isLinkEnabled());
            case CURATION -> alarmSetting.updateCuration(!alarmSetting.isCurationEnabled());
            case FOLDER -> alarmSetting.updateFolder(!alarmSetting.isFolderEnabled());
        }

        alarmSettingRepository.save(alarmSetting);

        // 토픽 구독은 NOTICE 계열만 해당 (LINK/FOLDER/CURATION은 개인 토큰 push)
        if (alarmSettingType == AlarmSettingType.NOTICE || alarmSettingType == AlarmSettingType.ALL) {
            boolean shouldSubscribe = alarmSetting.isEnabled(AlarmSettingType.NOTICE);
            eventPublisher.publishEvent(
                    new AlarmSettingChangedEvent(userId, alarmSettingType, shouldSubscribe, List.of("alarm-notice"))
            );
        }

        return alarmSetting.isEnabled(alarmSettingType);
    }

    // 알림 설정 타입별 조회
    public boolean viewAlarmSettingByType(Long userId, AlarmSettingType alarmSettingType) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));
        return alarmSetting.isEnabled(alarmSettingType);
    }

    // 개인 알림 전송 (알림 설정이 활성화된 경우에만 호출)
    // userId, 알림타입, targetId를 설정하면 메세지 내용을 자동으로 설정합니다.
    @Transactional
    public void sendAlarm(Long userId, AlarmRequestDTO.AlarmSendRequestDTO requestDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        AlarmType alarmType = requestDTO.type();

        String renderedBody = alarmType == AlarmType.CURATION_UPDATED
                ? String.format(alarmType.getBody(), user.getNickName())
                : alarmType.getBody();

        Alarm alarm = alarmRepository.save(Alarm.create(alarmType, requestDTO.targetId(), renderedBody));
        userAlarmRepository.save(UserAlarm.create(user, alarm));

        // CURATION 알림은 닉네임을 포함한 이벤트 발행
        PersonalAlarmEvent event = alarmType == AlarmType.CURATION_UPDATED
                ? PersonalAlarmEvent.ofWithNickname(userId, alarmType, requestDTO.targetId(), user.getNickName())
                : PersonalAlarmEvent.of(userId, alarmType, requestDTO.targetId());

        eventPublisher.publishEvent(event);
    }

    // 관리자 브로드캐스트 알림 등록, content 직접 입력
    @Transactional
    public void registerAdminAlarm(AlarmRequestDTO.AdminAlarmSendRequestDTO requestDTO) {
        AlarmType alarmType = requestDTO.type();
        if (alarmType.getSettingType() == AlarmSettingType.ALL) {
            throw new GeneralException(AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED);
        }

        // targetId는 임시로 설정 - 알림이 생성되고 나서 id를 targetId로 업데이트하여 보내야 하므로 entity에서는 의미없음
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

        if (alarmSettingType == null) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        if (size <= 0) {
            return new AlarmResponseDTO.AlarmCursorPageResponse(List.of(), null, false);
        }

        Pageable pageable = PageRequest.of(0, size + 1);
        Long safeCursor = (cursor == null) ? Long.MAX_VALUE : cursor;

        List<UserAlarm> fetched;
        if (alarmSettingType == AlarmSettingType.ALL) {
            fetched = userAlarmRepository.findAlarmListByCursor(userId, safeCursor, pageable);
        } else {
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
                        ua.getAlarm().getAlarmType().getSettingType(),
                        ua.getAlarm().getBody(),
                        ua.getCreatedAt(),
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
