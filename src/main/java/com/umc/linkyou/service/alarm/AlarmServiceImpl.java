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
@Transactional(readOnly = true)
public class AlarmServiceImpl implements AlarmService{
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;


    // FCM 토큰 등록
    @Override
    @Transactional
    public void registerFcmToken(Long userId, AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO) {
        //사용자 정보 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 새 토큰
        String newToken = alarmFcmTokenDTO.getFcmToken();

        // 중복검사
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, newToken);

        // 중복이 없을 경우 저장
        if (existingToken == null) {
            UsersFcmToken userFcmToken = UsersFcmToken.builder()
                    .user(user)
                    .fcmToken(newToken)
                    .build();
            userFcmTokenRepository.save(userFcmToken);
            return;
        }

        existingToken.activate();
    }

    // FCM 토큰 삭제
    /* 사용하는 경우
       1. 60일 이상 비활성일 경우
       2. 전송 실패할 경우
       3. 사용자가 회원탈퇴할 경우
     */
    @Override
    @Transactional
    public void deleteFcmToken(Long userId, String fcmToken) {
        // 기존 토큰 조회
        UsersFcmToken existingToken = userFcmTokenRepository.findByUser_IdAndFcmToken(userId, fcmToken);
        if (existingToken != null) {
            existingToken.deactivate();
        }
    }


}
