package com.umc.linkyou.web.dto.user;

import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Gender;
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
            @Schema(example = "MALE", allowableValues = {"MALE", "FEMALE"})
            @NotNull(message = "성별은 필수입니다") Gender gender,
            @Schema(example = "1") @NotNull Long jobId,
            @Schema(example = "[\"CAREER\", \"STUDY\"]") @NotEmpty(message = "목적 리스트는 최소 1개 이상 선택해야 합니다") List<String> purposeList,
            @Schema(example = "[\"IT\", \"DESIGN\"]") @NotEmpty(message = "관심사 리스트는 최소 1개 이상 선택해야 합니다") List<String> interestList,
            @Schema(description = "약관 동의 맵", example = "{\"TERMS_OF_USE\": true, \"PRIVACY_POLICY\": true, \"MARKETING\": false}")  Map<TermsType, Boolean> termsMap,
            @Schema(example = "ios-iphone-16-pro") @NotBlank(message = "deviceId는 필수입니다.") String deviceId,
            @Schema(example = "PHONE") @NotNull(message = "deviceType은 필수입니다.") DeviceType deviceType
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
        @Schema(description = "변경할 닉네임 (생략 시 변경하지 않음)", example = "새로운별명")
        private String nickname;

        @Schema(description = "변경할 직업 ID (생략 시 변경하지 않음, /users/me 응답의 job.id 참고)", example = "1")
        private Long jobId;                         // 현재 하고 있는 일

        @Schema(description = "변경할 링크 활용 목적 리스트 (생략 시 변경하지 않음, 필드를 보내면 전체 대체됨)",
                example = "[\"CAREER\", \"STUDY\"]",
                allowableValues = {"CAREER", "STUDY", "WORK", "SIDE_PROJECT", "SELF_DEVELOPMENT",
                        "LATER_READING", "INSIGHTS", "CREATION_REFERENCE", "OTHERS"})
        private List<String> purposes;              // 링크 활용 목적

        @Schema(description = "변경할 관심 콘텐츠 리스트 (생략 시 변경하지 않음, 필드를 보내면 전체 대체됨)",
                example = "[\"IT\", \"DESIGN\"]",
                allowableValues = {"BUSINESS", "IT", "DESIGN", "PSYCHOLOGY", "CAREER",
                        "CURRENT_EVENTS", "STUDY", "STARTUP", "SOCIETY", "WRITING", "INSIGHTS", "COLLECT"})
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

        @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE"})
        @NotNull(message = "성별은 필수입니다")
        private Gender gender;

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
        private  Map<TermsType, Boolean> termsMap;

        @Schema(example = "ios-iphone-16-pro")
        @NotBlank(message = "deviceId는 필수입니다.")
        private String deviceId;

        @Schema(example = "PHONE")
        @NotNull(message = "deviceType은 필수입니다.")
        private DeviceType deviceType;
    }

    /**
     * 회원 탈퇴 복구 요청 DTO
     * 복구 성공 시 바로 액세스/리프레시 토큰 쌍을 발급하기 위해 deviceId/deviceType을 함께 받는다.
     */
    @Builder
    public record RecoverDTO(
            @Schema(example = "ios-iphone-16-pro")
            @NotBlank(message = "deviceId는 필수입니다.")
            String deviceId,

            @Schema(example = "PHONE")
            @NotNull(message = "deviceType은 필수입니다.")
            DeviceType deviceType
    ) {
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
            Map<TermsType, Boolean> termsMap
    ) {
    }

}
