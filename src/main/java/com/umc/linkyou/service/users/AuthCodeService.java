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
        String token = stringRedisTemplate.opsForValue().get(redisKey);

        if (token == null) {
            log.warn("AuthCodeService: 유효하지 않거나 만료된 code={}", code);
            throw new GeneralException(ErrorStatus._INVALID_AUTH_CODE);
        }

        stringRedisTemplate.delete(redisKey); //1회용 즉시 삭제
        String[] tokens = token.split("::");
        return new UserResponseDTO.TokenPair(tokens[0], tokens[1]);

    }

}
