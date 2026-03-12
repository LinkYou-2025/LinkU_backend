package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCodeService {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String AUTH_CODE_KEY = "auth:code:";

    public UserResponseDTO.TokenPair exchangeCode(String code) {
        String redisKey = AUTH_CODE_KEY + code;
        String token = stringRedisTemplate.opsForValue().getAndDelete(redisKey);

        String[] tokens = token.split("::");
        if (tokens.length != 2) {
            log.error("AuthCodeService: Redis 토큰 형식 오류 code={}, token={}", code, token);
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
        return new UserResponseDTO.TokenPair(tokens[0], tokens[1]);

    }

}
