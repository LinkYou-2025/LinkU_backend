//package com.umc.linkyou.oauth2.web;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
//@Controller
//public class OAuthController {
//
//    // 👇 허용된 경로 리스트 (필요시 확장)
//    private static final List<String> ALLOWED_PATHS = List.of(
//            "/", "/mypage", "/profile", "/settings", "/cart",
//            "/reservations", "/notifications", "/search"
//    );
//
//    @GetMapping("/login/google")
//    public String googleLogin(HttpSession session, HttpServletRequest request) {
//        String targetPath = validatePath(request.getParameter("path"));
//        if (targetPath != null) {
//            session.setAttribute("oauth_target_path", targetPath);
//        }
//        return "redirect:/oauth2/authorization/google";
//    }
//
//    @GetMapping("/login/kakao")
//    public String kakaoLogin(HttpSession session, HttpServletRequest request) {
//        String targetPath = validatePath(request.getParameter("path"));
//        if (targetPath != null) {
//            session.setAttribute("oauth_target_path", targetPath);
//        }
//        return "redirect:/oauth2/authorization/kakao";
//    }
//
//    @GetMapping("/login/naver")
//    public String naverLogin(HttpSession session, HttpServletRequest request) {
//        String targetPath = validatePath(request.getParameter("path"));
//        if (targetPath != null) {
//            session.setAttribute("oauth_target_path", targetPath);
//        }
//        return "redirect:/oauth2/authorization/naver";
//    }
//
//    // 👇 핵심: path 검증 메서드
//    private String validatePath(String path) {
//        if (path == null || path.trim().isEmpty()) {
//            return null;
//        }
//
//        // 1. URL 디코드 (이중 인코딩 방지)
//        try {
//            path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            return null;
//        }
//
//        // 2. 상대경로(/로 시작) + 허용된 경로만
//        if (!path.startsWith("/") || path.length() > 100) {  // 길이 제한
//            return null;
//        }
//
//        // 3. 허용된 경로 체크 (접두사 매칭)
//        for (String allowed : ALLOWED_PATHS) {
//            if (path.startsWith(allowed)) {
//                return path;  // 안전!
//            }
//        }
//
//        return null;  // 기본 거부
//    }
//}
