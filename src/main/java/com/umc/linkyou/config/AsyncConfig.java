package com.umc.linkyou.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "defaultTaskExecutor")
    public Executor defaultTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(6);       // 동시에 돌릴 외부추천 작업 수
        ex.setMaxPoolSize(12);       // 일시 확장 한도
        ex.setQueueCapacity(10000);  // 월간 배치 큐 적재 한도
        ex.setThreadNamePrefix("async-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 과부하면 호출 스레드가 처리(스로틀링 효과)
        ex.initialize();
        return ex;
    }
    @Bean
    public Semaphore externalRecoLimiter() {
        return new Semaphore(6); // 동시에 6개만 실행
    }
}
