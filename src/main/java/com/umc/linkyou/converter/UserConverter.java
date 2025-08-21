package com.umc.linkyou.converter;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public class UserConverter {
    public static Users toUser(UserRequestDTO.JoinDTO request, Job job){
        Gender gender = null;
        switch(request.getGender()){
            case 1: gender = Gender.MALE; break;
            case 2: gender = Gender.FEMALE; break;
            //case 3: gender = Gender.NONE; break;
        }

        return new Users().builder()
                .nickName(request.getNickName())
                .email(request.getEmail())
                .password(request.getPassword())
                .gender(gender)
                .job(job)
                .status("ACTIVE")
                .build();
    }

    public static UserResponseDTO.JoinResultDTO toJoinResultDTO(Users users){
        return UserResponseDTO.JoinResultDTO.builder()
                .userId(users.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static UserResponseDTO.LoginResultDTO toLoginResultDTO(Users user, String accessToken, String refreshToken) {

        return new UserResponseDTO.LoginResultDTO().builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(user.getStatus())
                .inactiveDate(user.getInactiveDate())
                .build();
    }

    public static UserResponseDTO.UserProfileSummaryDto toUserInfoDTO(
            UserResponseDTO.UserProfileSummaryDto s,
            List<String> purposes,
            List<String> interests
    ) {
        return UserResponseDTO.UserProfileSummaryDto.builder()
                .nickName(s.getNickName())
                .email(s.getEmail())
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
