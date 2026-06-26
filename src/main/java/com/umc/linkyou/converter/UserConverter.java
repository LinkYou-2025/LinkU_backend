package com.umc.linkyou.converter;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class UserConverter {

    // 기존 toUser는 joinUser에서 사용
    public static Users toUser(UserRequestDTO.JoinDTO request, Job job){
        return Users.builder()
                .nickName(request.nickName())
                .gender(request.gender())
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
            List<String> interests,
            String loginProvider
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
                .loginProvider(loginProvider)
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


    /* 공통 메서드 */
    // 성별 변환 로직 공통 메서드
    public static Gender toGender(Integer genderCode) {
        if (genderCode == null || (genderCode != 1 && genderCode != 2)) {
            throw new GeneralException(UserErrorStatus._INVALID_GENDER);
        }
        return (genderCode == 1) ? Gender.MALE : Gender.FEMALE;
    }



    public static List<Purposes> toPurposes(Users user, List<String> purposeNames) {
        if (purposeNames == null || purposeNames.isEmpty()) return List.of();
        return purposeNames.stream()
                .map(name -> new Purposes(name, user))
                .toList();
    }

    // 엔티티의 초기 Interests 설정
    public static List<Interests> toInterests(Users user, List<String> interestNames) {
        if (interestNames == null || interestNames.isEmpty()) return List.of();
        return interestNames.stream()
                .map(name -> new Interests(name, user))
                .toList();
    }
}
