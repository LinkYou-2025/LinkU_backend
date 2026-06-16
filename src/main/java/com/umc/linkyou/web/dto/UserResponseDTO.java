package com.umc.linkyou.web.dto;

import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserResponseDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinResultDTO{
        Long userId;

        LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDTO{
        Long userId;
        String accessToken;
        String refreshToken; // 리프레시 토큰
        UserStatus status;
        LocalDateTime inactiveDate;
    }

    // 리프레시 토큰 로테이션
    @Getter @AllArgsConstructor
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO{
        String nickName;

        String email;

        Gender gender;

        Job job;

        Long myLinku; // 나의 링크

        Long myFolder; // 나의 폴더

        // 내가 만든 ai 링크
        Boolean myAiLinku;

    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class withDrawalResultDTO{
        Long userId;
        String nickname;
        LocalDateTime createdAt;
        UserStatus status;
        LocalDateTime inactiveDate;
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
        //QueryDSL 7개 파라미터용 생성자
        public UserProfileSummaryDto(String nickName, String email, Gender gender, Job job,
                                     Long myLinku, Long myFolder, Long myAiLinku) {
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
    public record NicknameDTO(
            @Schema(description = "사용자 닉네임", example = "링큐유저")
            String nickname
    ) {
    }

    @Getter @Setter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "약관 동의 상태 응답 (키-값 형태)")
    public static class TermsStatusDTO {

        @Schema(description = "사용자 ID", example = "123")
        private Long userId;

        @Schema(description = "약관별 동의 상태 맵",
                example = "{\"TERMS_OF_USE\":true,\"MARKETING\":true,\"PRIVACY_POLICY\":false}")
        private Map<TermsType, Boolean> termsStatus;

        @Schema(description = "필수 약관 모두 동의했는지 여부", example = "true")
        private boolean allRequiredAgreed;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "개별 약관 동의 정보")
    public static class TermsAgreementDTO {

        @Schema(description = "약관 타입", example = "TERMS_OF_USE")
        private String termsType;

        @Schema(description = "필수 약관 여부", example = "true")
        private boolean isRequired;

        @Schema(description = "약관 버전", example = "v1.0")
        private String termsVersion;

        @Schema(description = "동의 시점", example = "2026-02-12T06:28:00")
        private LocalDateTime agreedAt;

        @Schema(description = "현재 동의 상태", example = "true")
        private boolean isAgreed;
    }
}
