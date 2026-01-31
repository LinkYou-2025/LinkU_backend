package com.umc.linkyou.oauth;

import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.oauth.utils.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;         // ← 추가!
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.deeplink.base-url}")
    private String deepLinkBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        // JWT 생성
        String accessToken = jwtTokenProvider.createAccessToken(email);

        // session에서 targetPath 가져오기
        HttpSession session = request.getSession(false);

        String targetPath = null;
        if (session != null) {
            targetPath = (String) session.getAttribute("oauth_target_path");
            session.removeAttribute("oauth_target_path");

            // path를 통한 보안문제
            if (targetPath != null &&
                    (!targetPath.startsWith("/") || targetPath.length() > 100)) {
                targetPath = null;  // 안전하지 않음
            }
        }


        // 항상 딥링크 생성 (targetPath 포함)
        String deepLinkUrl = String.format("%s/auth?path=%s&token=%s",
                deepLinkBaseUrl,
                URLEncoder.encode(targetPath != null ? targetPath : "/", StandardCharsets.UTF_8),
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
        );

        log.info("OAuth → 딥링크: {} (user={})", deepLinkUrl, email);
        response.sendRedirect(deepLinkUrl);
    }
}
