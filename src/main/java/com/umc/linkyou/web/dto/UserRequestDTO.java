package com.umc.linkyou.web.dto;

import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Interest;
import com.umc.linkyou.domain.enums.Purpose;
import com.umc.linkyou.domain.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

public class UserRequestDTO {

    @Builder // 테스트 코드에서 편리하게 생성하기 위해 추가
    public record JoinDTO(
            @Schema(example = "별명") @NotBlank String nickName,
            @Schema(example = "example@gmail.com") @NotBlank @Email String email,
            @Schema(example = "zaq123") @NotBlank String password,
            @Schema(example = "1") @NotNull Integer gender,
            @Schema(example = "1") @NotNull Long jobId,
            @Schema(example = "[\"CAREER\", \"STUDY\"]") @NotEmpty(message = "목적 리스트는 최소 1개 이상 선택해야 합니다") List<String> purposeList,
            @Schema(example = "[\"IT\", \"DESIGN\"]") @NotEmpty(message = "관심사 리스트는 최소 1개 이상 선택해야 합니다") List<String> interestList,
            @Schema(description = "약관 동의 맵", example = "{\"TERMS_OF_USE\": true, \"PRIVACY_POLICY\": true, \"MARKETING\": false}") Map<String, Boolean> termsMap
    ) {}

    public record LoginRequestDTO(
            @Schema(example = "example@gmail.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            String email,

            @Schema(example = "zaq123")
            @NotBlank(message = "패스워드는 필수입니다.")
            String password,

            @Schema(example = "ios-iphone-16-pro")
            @NotBlank(message = "deviceId는 필수입니다.")
            String deviceId,

            @Schema(example = "PHONE")
            @NotNull(message = "deviceType은 필수입니다.")
            DeviceType deviceType
    ) {
    }

    public record TokenReissueRequestDTO(
            @Schema(example = "jwt_refresh_token")
            @NotBlank(message = "리프레시 토큰은 필수입니다.")
            String refreshToken,

            @Schema(example = "ios-iphone-16-pro")
            @NotBlank(message = "deviceId는 필수입니다.")
            String deviceId
    ) {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileDTO {
        private String nickname;
        private Long jobId;                         // 현재 하고 있는 일
        private List<String> purposes;              // 링크 활용 목적
        private List<String> interests;             // 관심 콘텐츠
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteReasonDTO{
        @Schema(example = "회원탈퇴 이유에 대한 상세설명")
        private String reason;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialCompleteDTO {  // ← 신규 추가!

        @Schema(description = "닉네임 (중복 불가)", example = "linkyou_user")
        @NotBlank(message = "닉네임은 필수입니다")
        private String nickName;

        @Schema(description = "성별 (1=남성, 2=여성)", example = "1")
        @NotNull(message = "성별은 필수입니다")
        private Integer gender;

        @Schema(description = "직업 ID", example = "1")
        @NotNull(message = "직업은 필수입니다")
        private Long jobId;

        @Schema(description = "사용 목적 리스트", example = "[\"CAREER\", \"STUDY\"]")
        @NotEmpty(message = "목적 리스트는 최소 1개 이상 선택해야 합니다")
        private List<String> purposeList;

        @Schema(description = "관심사 리스트", example = "[\"IT\", \"DESIGN\"]")
        @NotEmpty(message = "관심사 리스트는 최소 1개 이상 선택해야 합니다")
        private List<String> interestList;

        @Schema(description = "약관 동의 맵", example = "{\"TERMS_OF_USE\": true, \"PRIVACY_POLICY\": true, \"MARKETING\": false}")
        @NotEmpty(message = "약관 동의 정보는 필수입니다.")
        private Map<String, Boolean> termsMap;
    }

    /**
     * 약관 일괄 동의/변경 처리 DTO
     */
    @Builder
    public record TermsAgreeDTO(
            @Schema(
                    description = "변경할 약관 상태 맵 (Key: 약관타입, Value: 동의여부)",
                    example = "{\"TERMS_OF_USE\": true, \"PRIVACY_POLICY\": true, \"MARKETING\": false}",
                    anyOf = {TermsType.class}
            )
            @NotEmpty(message = "변경할 약관 상태를 하나 이상 입력해주세요.")
            Map<String, Boolean> termsMap,

            @Schema(description = "약관 버전", example = "v1.0")
            String termsVersion
    ) {
        // 컴팩트 생성자를 사용하여 기본값 설정
        public TermsAgreeDTO {
            if (termsVersion == null) termsVersion = "v1.0";
        }
    }

}
