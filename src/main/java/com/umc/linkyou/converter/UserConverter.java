package com.umc.linkyou.converter;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class UserConverter {
    public static Users toUser(UserRequestDTO.JoinDTO request, Job job){
        log.info("toUser gender input: {}", request.getGender());

        Integer genderInt = request.getGender();
        if (genderInt == null || (genderInt != 1 && genderInt != 2)) {
            log.error("Invalid gender: {}", genderInt);
            throw new GeneralException(ErrorStatus._INVALID_GENDER);
        }

        // 2. 🔥 올바른 변환
        Gender gender = genderInt == 1 ? Gender.MALE : Gender.FEMALE;
        log.info("Converted gender: {}", gender);
        return Users.builder()
                .nickName(request.getNickName())
                .password(request.getPassword())
                .gender(gender)
                .job(job)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static UserResponseDTO.JoinResultDTO toJoinResultDTO(Users users){
        return UserResponseDTO.JoinResultDTO.builder()
                .userId(users.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static UserResponseDTO.LoginResultDTO toLoginResultDTO(Users user, String accessToken, String refreshToken) {

        return UserResponseDTO.LoginResultDTO.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(user.getStatus())
                .inactiveDate(user.getInactiveDate())
                .build();
    }

    public static UserResponseDTO.UserProfileSummaryDto toUserInfoDTO(
            UserResponseDTO.UserProfileSummaryDto s,
            String email,
            List<String> purposes,
            List<String> interests
    ) {
        return UserResponseDTO.UserProfileSummaryDto.builder()
                .nickName(s.getNickName())
                .email(email != null ? email : s.getEmail())  // 우선순위: 인자 > 기존
                .gender(s.getGender())
                .job(s.getJob())
                .myLinku(s.getMyLinku())
                .myFolder(s.getMyFolder())
                .myAiLinku(s.getMyAiLinku())
                .purposes(purposes)
                .interests(interests)
                .build();
    }

    public static UserResponseDTO.withDrawalResultDTO toWithDrawalResultDTO(Users user) {
        if (user == null) return null;
        return UserResponseDTO.withDrawalResultDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickName())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .inactiveDate(user.getInactiveDate())
                .build();
    }


}
