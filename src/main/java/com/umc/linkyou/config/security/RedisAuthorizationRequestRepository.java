package com.umc.linkyou.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "oauth2:state:";
    private static final Duration TTL = Duration.ofMinutes(10); // state 유효 시간

    private String key(HttpServletRequest request) {
        // state 파라미터로 키 구성 (저장/로드/삭제 모두 동일 키 사용)
        String state = request.getParameter("state");
        return KEY_PREFIX + (state != null ? state : "unknown");
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        try {
            String state = authorizationRequest.getState();
            String json = objectMapper.writeValueAsString(authorizationRequest);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + state, json, TTL);
            log.debug("OAuth2 AuthorizationRequest saved. state={}", state);
        } catch (Exception e) {
            log.error("OAuth2 AuthorizationRequest 저장 실패", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(request));
            if (json == null) return null;
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.error("OAuth2 AuthorizationRequest 로드 실패", e);
            return null;
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String redisKey = key(request);
            String json = stringRedisTemplate.opsForValue().getAndDelete(redisKey);
            if (json == null) return null;
            log.debug("OAuth2 AuthorizationRequest 삭제. key={}", redisKey);
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.error("OAuth2 AuthorizationRequest 삭제 실패", e);
            return null;
        }
    }
}
