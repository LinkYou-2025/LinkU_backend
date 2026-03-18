package com.umc.linkyou.config.security;

import com.umc.linkyou.config.security.jwt.JwtAuthenticationFilter;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
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
//    private final OAuth2UserServiceImpl oAuth2UserService;
//    private final OAuth2TokenClient oAuth2TokenClient;
//    @Lazy
//    private final OAuth2SuccessHandler oAuth2SuccessHandler;
//    private final RedisAuthorizationRequestRepository redisAuthorizationRequestRepository;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/css/**",
                                "/api/v1/users/**",
                                "/api/v1/auth/token/exchange",
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/*.well-known/**",
                                "/open/**",
                                "/actuator/**",
                                "/api/v1/auth/mobile/**",
                                "/error/**",
                                "/docs/**"
//                                ,"/oauth2/**",          // 소셜 로그인 진입점 (/oauth2/authorization/{registrationId})
//                                "/api/oauth2/**",     // 소셜 로그인 에러, 성공 콜백
//                                "/login/kakao",
//                                "/login/google",
//                                "/login/naver"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // ← IF_REQUIRED → STATELESS
                )

//                .oauth2Login(oauth2 -> oauth2
//                        .authorizationEndpoint(authorization -> authorization
//                                .baseUri("/oauth2/authorization")
//                                // STATELESS여도 OAuth2 state는 세션에 저장하도록 명시
//                                .authorizationRequestRepository(redisAuthorizationRequestRepository)
//                        )
//                        .redirectionEndpoint(redirection -> redirection
//                                .baseUri("/login/oauth2/code/*")
//                        )
//                        .tokenEndpoint(token -> token
//                                .accessTokenResponseClient(oAuth2TokenClient)
//                        )
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(oAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                )
//                .requiresChannel(channel -> channel
//                        .anyRequest().requiresSecure()
//                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 정책 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
