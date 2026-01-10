package com.umc.linkyou.config.security;

import com.umc.linkyou.config.security.jwt.JwtAuthenticationFilter;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.oauth.OAuth2TokenClient;
import com.umc.linkyou.config.security.oauth.OAuth2UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2TokenClient oAuth2TokenClient;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/css/**",
                                "/api/users/**",
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/*.well-known/**",
                                "/open/**",
                                "/oauth2/**",          // 소셜로그인 진입점
                                "/api/oauth2/**",    // 소셜로그인 에러, 성공 콜백 url
                                "/actuator/**",
                                "/error/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .oauth2Login(oauth2 -> oauth2
                                // /oauth2/authorization/{registrationId} 진입 URI
                                .authorizationEndpoint(authorization -> authorization
                                        .baseUri("/oauth2/authorization")
                                )
                                // /login/oauth2/code/{registrationId} 콜백 URI 패턴
                                .redirectionEndpoint(redirection -> redirection
                                        .baseUri("/login/oauth2/code/*")
                                )
                                // Authorization Code → Access Token 교환 시 사용할 클라이언트
                                .tokenEndpoint(token -> token
                                        .accessTokenResponseClient(oAuth2TokenClient)
                                )
                                // Access Token → userInfo 조회 후 Users/AuthAccount 매핑
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(oAuth2UserService)
                                )
                        // 필요하면 여기서 JWT 발급/리다이렉트 핸들러도 추가 가능
                        // .successHandler(oAuth2SuccessHandler)
                        // .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 정책을 정의하는 빈
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));  // 모든 출처 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH")); // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 쿠키 등 인증정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
