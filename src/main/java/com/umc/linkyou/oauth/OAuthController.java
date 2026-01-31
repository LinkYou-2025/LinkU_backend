package com.umc.linkyou.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthController {

    @GetMapping("/login/google")
    public String googleLogin(HttpSession session, HttpServletRequest request) {
        // 👇 새로 추가: path를 session에 저장
        String targetPath = request.getParameter("path");
        if (targetPath != null && !targetPath.isEmpty()) {
            session.setAttribute("oauth_target_path", targetPath);
        }
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/login/kakao")
    public String kakaoLogin(HttpSession session, HttpServletRequest request) {
        String targetPath = request.getParameter("path");
        if (targetPath != null && !targetPath.isEmpty()) {
            session.setAttribute("oauth_target_path", targetPath);
        }
        return "redirect:/oauth2/authorization/kakao";
    }

    @GetMapping("/login/naver")
    public String naverLogin(HttpSession session, HttpServletRequest request) {
        String targetPath = request.getParameter("path");
        if (targetPath != null && !targetPath.isEmpty()) {
            session.setAttribute("oauth_target_path", targetPath);
        }
        return "redirect:/oauth2/authorization/naver";
    }

    @GetMapping("/login/success")
    public String loginSuccess() {
        return "login-success";
    }
}
