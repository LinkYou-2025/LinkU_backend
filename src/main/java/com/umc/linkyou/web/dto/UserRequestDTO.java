package com.umc.linkyou.web.dto;

import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.Interest;
import com.umc.linkyou.domain.enums.Purpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class UserRequestDTO {

    @Getter
    @Setter
    public static class JoinDTO {

        @Schema(example = "별명")
        @NotBlank
        String nickName;

        @Schema(example = "example@gmail.com")
        @NotBlank
        @Email
        String email;

        @Schema(example = "zaq123")
        @NotBlank
        String password;

        @Schema(example = "1")
        @NotNull
        Integer gender;

        @Schema(example = "1")
        @NotNull
        Long jobId;

        @Schema(example = "[\"CAREER\", \"STUDY\"]")
        @NotNull(message = "목적 리스트는 필수입니다")
        List<String> purposeList;

        @Schema(example = "[\"IT\", \"DESIGN\"]")
        @NotNull(message = "관심사 리스트는 필수입니다")
        List<String> interestList;
    }

    @Getter
    @Setter
    public static class LoginRequestDTO {
        @Schema(example = "example@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        private String email;

        @Schema(example = "zaq123")
        @NotBlank(message = "패스워드는 필수입니다.")
        private String password;

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
        @NotNull(message = "목적 리스트는 필수입니다")
        private List<String> purposeList;

        @Schema(description = "관심사 리스트", example = "[\"IT\", \"DESIGN\"]")
        @NotNull(message = "관심사 리스트는 필수입니다")
        private List<String> interestList;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermsAgreeDTO {
        @Schema(example = "[\"TERMS_OF_USE\", \"PRIVACY_POLICY\", \"MARKETING\"]")
        @NotNull(message = "약관 동의 목록은 필수입니다")
        private List<String> termsTypes;

        @Schema(example = "v1.0")
        @NotNull(message = "약관 버전은 필수입니다")
        private String termsVersion;
    }
    @Getter @Setter
    public static class SingleTermUpdateDTO {
        @Schema(example = "MARKETING")
        @NotNull
        private String termsType;

        @Schema(example = "true")
        private Boolean isAgreed;
    }
}
