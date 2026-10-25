package com.umc.linkyou.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlackListManagerTest {

    @InjectMocks private AccessTokenBlackListManager accessTokenBlackListManager;

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("addSessionToBlacklist")
    class AddSessionToBlacklist {

        @Test
        @DisplayName("유효한 sid를 세션 블랙리스트 키와 TTL로 저장한다")
        void 유효한_sid를_세션_블랙리스트_키와_TTL로_저장한다() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            accessTokenBlackListManager.addSessionToBlacklist("session-1", 60_000L);

            // then
            verify(valueOperations)
                    .set(
                            eq("blacklist:access:session:session-1"),
                            eq("logout"),
                            eq(Duration.ofMillis(60_000L)));
        }

        @Test
        @DisplayName("sid가 없거나 TTL이 없으면 블랙리스트에 저장하지 않는다")
        void sid가_없거나_TTL이_없으면_블랙리스트에_저장하지_않는다() {
            // when
            accessTokenBlackListManager.addSessionToBlacklist("", 60_000L);
            accessTokenBlackListManager.addSessionToBlacklist("session-1", 0L);

            // then
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklisted {

        @Test
        @DisplayName("토큰의 sid가 세션 블랙리스트에 있으면 true를 반환한다")
        void 토큰의_sid가_세션_블랙리스트에_있으면_true를_반환한다() {
            // given
            when(jwtTokenProvider.getSessionId("access-token")).thenReturn("session-1");
            when(redisTemplate.hasKey("blacklist:access:session:session-1")).thenReturn(true);

            // when
            boolean result = accessTokenBlackListManager.isBlacklisted("access-token");

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("빈 토큰이면 Redis를 조회하지 않고 false를 반환한다")
        void 빈_토큰이면_Redis를_조회하지_않고_false를_반환한다() {
            // when
            boolean result = accessTokenBlackListManager.isBlacklisted(" ");

            // then
            assertFalse(result);
            verify(jwtTokenProvider, never()).getSessionId(" ");
            verifyNoInteractions(redisTemplate);
        }
    }
}
