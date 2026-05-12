package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.service.email.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PasswordResetPageController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/password/reset")
    public String passwordResetPage(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        model.addAttribute("token", token);
        return "email/password-reset-form";
    }

    @PostMapping("/password/reset")
    public String handlePasswordReset(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model) {
        try {
            passwordResetService.resetPassword(token, newPassword, confirmPassword);
            model.addAttribute("success", true);
        } catch (UserHandler e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getErrorReasonHttpStatus().getMessage());
        }
        return "email/password-reset-form";
    }
}
