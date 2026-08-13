package com.umc.linkyou.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetRequestDTO {

    @NotBlank(message = "token은 필수입니다.")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$",
            message = "비밀번호는 8~64자의 영문 대문자, 영문 소문자, 숫자, 특수문자를 각각 포함해야 합니다.")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String confirmPassword;
}
