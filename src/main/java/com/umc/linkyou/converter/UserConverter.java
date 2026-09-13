package com.umc.linkyou.converter;

import java.util.List;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.mapping.UsersInterest;
import com.umc.linkyou.domain.mapping.UsersPurpose;
import com.umc.linkyou.web.dto.user.UserRequestDTO;
import com.umc.linkyou.web.dto.user.UserResponseDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserConverter {

    // 기존 toUser는 joinUser에서 사용
    public static Users toUser(UserRequestDTO.JoinDTO request, Job job) {
        // joinUser는 컨트롤러 밖에서도 직접 호출될 수 있어 서비스 경계에서 gender null 방어
        if (request.gender() == null) {
            throw new UserHandler(UserErrorStatus._INVALID_GENDER);
        }
        return Users.builder()
                .nickName(request.nickName())
                .gender(request.gender())
                .job(job)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static UserResponseDTO.JoinResultDTO toJoinResultDTO(
            Users users, String accessToken, String refreshToken) {
        return UserResponseDTO.JoinResultDTO.builder()
                .userId(users.getId())
                .createdAt(users.getCreatedAt())
                .tokenResponse(new UserResponseDTO.TokenPair(accessToken, refreshToken))
                .build();
    }

    public static UserResponseDTO.LoginResultDTO toLoginResultDTO(
            Users user, String accessToken, String refreshToken) {

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
            String loginProvider) {
        return UserResponseDTO.UserProfileSummaryDto.builder()
                .nickName(s.getNickName())
                .email(email != null ? email : s.getEmail()) // 우선순위: 인자 > 기존
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
        return toWithDrawalResultDTO(user, null, null);
    }

    // 회원복구 성공 시 accessToken/refreshToken을 함께 담기 위한 오버로드
    public static UserResponseDTO.withDrawalResultDTO toWithDrawalResultDTO(
            Users user, String accessToken, String refreshToken) {
        if (user == null) return null;
        return UserResponseDTO.withDrawalResultDTO
                .builder()
                .userId(user.getId())
                .nickname(user.getNickName())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .inactiveDate(user.getInactiveDate())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // Users - Purposes 다대다 조인 엔티티 생성 (마스터 엔티티는 서비스 레이어에서 조회/생성 후 전달받음)
    public static List<UsersPurpose> toUsersPurposes(Users user, List<Purposes> purposes) {
        if (purposes == null || purposes.isEmpty()) return List.of();
        return purposes.stream().map(purpose -> UsersPurpose.of(user, purpose)).toList();
    }

    // Users - Interests 다대다 조인 엔티티 생성 (마스터 엔티티는 서비스 레이어에서 조회/생성 후 전달받음)
    public static List<UsersInterest> toUsersInterests(Users user, List<Interests> interests) {
        if (interests == null || interests.isEmpty()) return List.of();
        return interests.stream().map(interest -> UsersInterest.of(user, interest)).toList();
    }
}
