package com.umc.linkyou.config.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// 시간 의존 로직(TTL 만료 판단 등)을 테스트에서 제어할 수 있도록 Clock을 빈으로 등록한다.
// 운영 코드는 항상 이 systemUTC 빈을 주입받고, 테스트는 생성자에 Clock.fixed()/커스텀 Clock을 직접 넘긴다.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
