package com.umc.linkyou.web.dto.user;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.domain.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class UserResponseDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinResultDTO {
        Long userId;
        Integer createdAt;
        TokenPair tokenResponse;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDTO {
        Long userId;
        String accessToken;
        String refreshToken; // 리프레시 토큰
        UserStatus status;
        LocalDateTime inactiveDate;
        Integer createdAt;
    }

    // 리프레시 토큰 로테이션
    @Getter
    @AllArgsConstructor
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class withDrawalResultDTO {
        Long userId;
        String nickname;
        LocalDateTime createdAt;
        UserStatus status;
        LocalDateTime inactiveDate;
        String accessToken;
        String refreshToken;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Setter
    public static class UserProfileSummaryDto {
        private final String nickName;
        private final String email;
        private final Gender gender;
        private final Job job;
        private final Long myLinku;
        private final Long myFolder;
        private final Long myAiLinku;
        private String loginProvider;
        private List<String> purposes;
        private List<String> interests;

        // QueryDSL 7개 파라미터용 생성자
        public UserProfileSummaryDto(
                String nickName,
                String email,
                Gender gender,
                Job job,
                Long myLinku,
                Long myFolder,
                Long myAiLinku) {
            this.nickName = nickName;
            this.email = email;
            this.gender = gender;
            this.job = job;
            this.myLinku = myLinku;
            this.myFolder = myFolder;
            this.myAiLinku = myAiLinku;
            this.loginProvider = null;
            this.purposes = Collections.emptyList();
            this.interests = Collections.emptyList();
        }
    }

    @Schema(description = "닉네임 조회 응답")
    public record NicknameDTO(@Schema(description = "사용자 닉네임", example = "링큐유저") String nickname) {}

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 동의 상태 응답 (키-값 형태)")
    public static class TermsStatusDTO {

        @Schema(
                description = "약관별 동의 상태 맵",
                example = "{\"TERMS_OF_USE\":true,\"MARKETING\":true,\"PRIVACY_POLICY\":false}")
        private Map<TermsType, Boolean> termsStatus;

        @Schema(description = "필수 약관 모두 동의했는지 여부", example = "true")
        private boolean allRequiredAgreed;
    }
}
