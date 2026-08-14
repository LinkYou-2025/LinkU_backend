package com.umc.linkyou.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.domain.enums.DeviceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("RefreshTokenManager Redis Lua 통합 테스트")
class RefreshTokenManagerIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private RefreshTokenManager refreshTokenManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        refreshTokenManager = new RefreshTokenManager(redisTemplate, objectMapper);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Nested
    @DisplayName("saveToken")
    class SaveToken {

        @Test
        @DisplayName("네 번째 기기 로그인 시 가장 오래된 세션을 반환한다")
        void 네_번째_기기_로그인_시_가장_오래된_세션을_반환한다() throws InterruptedException {
            // given
            saveToken("device-1", "session-1");
            Thread.sleep(5);
            saveToken("device-2", "session-2");
            Thread.sleep(5);
            saveToken("device-3", "session-3");

            // when
            Optional<RefreshTokenManager.InvalidatedSession> invalidatedSession =
                    saveToken("device-4", "session-4");

            // then
            assertEquals("session-1", invalidatedSession.orElseThrow().sessionId());
            assertTrue(redisTemplate.hasKey("blacklist:access:session:session-1"));
        }

        @Test
        @DisplayName("같은 기기로 다시 로그인하면 기존 세션을 반환한다")
        void 같은_기기로_다시_로그인하면_기존_세션을_반환한다() {
            // given
            saveToken("device-1", "session-1");

            // when
            Optional<RefreshTokenManager.InvalidatedSession> invalidatedSession =
                    saveToken("device-1", "session-2");

            // then
            assertEquals("session-1", invalidatedSession.orElseThrow().sessionId());
            assertTrue(redisTemplate.hasKey("blacklist:access:session:session-1"));
        }

        @Test
        @DisplayName("구형 세션 레코드를 퇴출하면 블랙리스트 등록 없이 빈 결과를 반환한다")
        void 구형_세션_레코드를_퇴출하면_블랙리스트_등록_없이_빈_결과를_반환한다() throws Exception {
            // given
            long now = System.currentTimeMillis();
            redisTemplate.opsForHash().put(
                    "sessions:1:KAKAO",
                    "legacy-device",
                    objectMapper.writeValueAsString(Map.of(
                            "tokenId", "legacy-refresh-token-id",
                            "deviceType", DeviceType.PHONE.name(),
                            "createdAt", now - 100L,
                            "expiresAt", now + 1_209_600_000L)));
            saveToken("device-2", "session-2");
            saveToken("device-3", "session-3");

            // when
            Optional<RefreshTokenManager.InvalidatedSession> invalidatedSession =
                    saveToken("device-4", "session-4");

            // then
            assertTrue(invalidatedSession.isEmpty());
            assertTrue(redisTemplate.keys("blacklist:access:session:*").isEmpty());
        }
    }

    private Optional<RefreshTokenManager.InvalidatedSession> saveToken(
            String deviceId, String sessionId) {
        long now = System.currentTimeMillis();
        return refreshTokenManager.saveToken(
                1L,
                "KAKAO",
                deviceId,
                "refresh-token-id-" + sessionId,
                DeviceType.PHONE,
                now + 1_209_600_000L,
                sessionId,
                now + 3_600_000L);
    }
}
