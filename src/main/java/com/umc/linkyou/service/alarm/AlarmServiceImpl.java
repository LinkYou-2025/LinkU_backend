package com.umc.linkyou.service.alarm;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService{
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerFcmToken(Long userId, AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO) {
        //사용자 정보 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 새 토큰
        String newToken = alarmFcmTokenDTO.getFcmToken();

        //중복검사
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, newToken);

        //중복이 없을 경우 저장
        if (existingToken == null) {
            UsersFcmToken userFcmToken = UsersFcmToken.builder()
                    .user(user)
                    .fcmToken(newToken)
                    .build();
            userFcmTokenRepository.save(userFcmToken);
        }

    }
}
