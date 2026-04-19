package com.umc.linkyou.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailRequestDTO {

    public record CodeSendDTO(
            @Schema(example = "example@gmail.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            String email
    ) {}

    public record CodeVerifyDTO(
            @Schema(example = "example@gmail.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            String email,

            @Schema(example = "123456")
            @NotBlank(message = "인증 코드는 필수입니다.")
            String code
    ) {}

    public record ResetLinkDTO(
            @Schema(example = "example@gmail.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            String email
    ) {}
}
