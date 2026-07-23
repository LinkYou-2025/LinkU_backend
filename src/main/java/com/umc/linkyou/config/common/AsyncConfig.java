package com.umc.linkyou.config.common;

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
        ex.setCorePoolSize(10);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(300);
        ex.setThreadNamePrefix("async-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
    @Bean
    public Semaphore externalRecoLimiter() {
        return new Semaphore(3);
    }

    @Bean
    public Semaphore internalRecoLimiter() {
        return new Semaphore(6);
    }

    @Bean
    public Semaphore mentLimiter() {
        return new Semaphore(3);
    }


    @Bean(name = "fcmTaskExecutor")
    public Executor fcmTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("fcm-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    // 알림 브로드캐스트용
    @Bean(name = "alarmBatchTaskExecutor")
    public Executor alarmBatchTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("alarm-batch-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(60);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }


    /**
     * 외부 추천 큐레이션 이미지 fetch 전용
     * default와 같이 사용하면 해당 스레드에서 이 결과를 join으로 기다리기 때문에,
     * 데드락 발생 가능성 있으므로 별도 풀로 분리
     */
    @Bean(name = "imageFetchTaskExecutor")
    public Executor imageFetchTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(15);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("image-fetch-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}
