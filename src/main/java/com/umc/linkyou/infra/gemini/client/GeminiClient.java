package com.umc.linkyou.infra.gemini.client;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.ai.AiClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient implements AiClient {

    // 검색 도구 없는 순수 생성 호출용
    private static final int GEMINI_TIMEOUT_SECONDS = 15;
    // Google Search grounding이 붙는 경우, 검색 결과 수집에 시간이 더 걸리므로 타임아웃을 길게 잡음
    private static final int GEMINI_SEARCH_TIMEOUT_SECONDS = 25;

    private final ObjectProvider<Client> clientProvider;

    // 링크 생성/아티클 요약 전용 - 사용자 요청 경로라 큐레이션 배치 폭주에 영향받지 않도록 분리
    private final ExecutorService geminiLinkExecutor = Executors.newFixedThreadPool(6);
    // 큐레이션 멘트/외부추천(completionCreative, completionWithSearch) 전용

    // 멘트추천, 외부추천으로 최대 동시 호출 6개 기준으로 크기를 맞춤
    private final ExecutorService geminiCurationExecutor = Executors.newFixedThreadPool(6);
    private final AtomicLong callSeq = new AtomicLong();

    @Value("${gemini.model.name}")
    private String modelName;

    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "fallback")
    public String completion(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.3f, GEMINI_TIMEOUT_SECONDS, geminiLinkExecutor);
    }

    // 창의적인 응답 생성
    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "fallback")
    public String completionCreative(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.9f, GEMINI_TIMEOUT_SECONDS, geminiCurationExecutor);
    }

    // Google Search로 실시간 정보 반영
    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "fallback")
    public String completionWithSearch(String systemInstruction, String userPrompt)
    {
        return generate(
                systemInstruction, userPrompt, GOOGLE_SEARCH_TOOL, 2048, 0.3f,
                GEMINI_SEARCH_TIMEOUT_SECONDS, geminiCurationExecutor);
    }

    /**
     * ai client 공통 fallback
     * 서킷 OPEN이거나 generate()가 실패했을 때 Resilience4j가 원본 메서드 대신 이 메서드를 사용
     */
    private String fallback(String systemInstruction, String userPrompt, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("[GEMINI] 서킷 OPEN 상태로 요청 거부");
            throw new GeneralException(GeminiErrorStatus.GEMINI_TOO_MANY_REQUESTS);
        }
        if (t instanceof GeneralException ge) {
            throw ge;
        }
        log.error("[GEMINI] fallback 처리, 알 수 없는 오류", t);
        throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
    }

    /**
     * executor를 분리하여 동시 호출 제한, 큐 길이 제한, 타임아웃 등을 제어
     * 일반적으로는 이 메서드를 사용, 실패 시 fallback()이 호출됨
     */
    private String generate(
            String systemInstruction, String userPrompt, Tool tool, int maxTokens, float temp,
            int timeoutSeconds, ExecutorService executor)
    {
        Client client = clientProvider.getIfAvailable();
        if (client == null) {
            log.warn("Gemini Client 비활성 상태에서 호출됨");
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }

        GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                .maxOutputTokens(maxTokens)
                .temperature(temp)
                .httpOptions(HttpOptions.builder().timeout(timeoutSeconds * 1000).build());

        if (tool != null) {
            builder.tools(Collections.singletonList(tool));
        }
        GenerateContentConfig config = builder.build();

        long callId = callSeq.incrementAndGet();
        long submittedAt = System.currentTimeMillis();
        logExecutorStats(callId, "제출 전", executor);

        // 타임아웃 없이 동기 블로킹이라 응답이 오지 않으면 세마포어 영구고갈 가능 -> 별도 스레드로 던지고 정해진 시간만 대기 후 포기하도록 강제
        Future<String> future = executor.submit(
                () -> client.models.generateContent(modelName, userPrompt, config).text());
        log.info("[GEMINI] call={} 제출 완료 thread={}", callId, Thread.currentThread().getName());

        try {
            String result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("[GEMINI] call={} 완료 elapsed={}ms", callId, System.currentTimeMillis() - submittedAt);
            return result;
        } catch (TimeoutException e) {
            boolean cancelled = future.cancel(true);
            log.error(
                    "[GEMINI] call={} {}초 초과 (cancel 결과={}), elapsed={}ms",
                    callId, timeoutSeconds, cancelled, System.currentTimeMillis() - submittedAt);
            logExecutorStats(callId, "타임아웃 직후", executor);
            throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
        } catch (Exception e) {
            log.error(
                    "[GEMINI] call={} 호출 오류, elapsed={}ms",
                    callId, System.currentTimeMillis() - submittedAt, e);
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }

    /**
     * executor 상태 로깅
     * - 큐가 쌓이거나 active 스레드가 poolSize에 계속 붙어 있으면 실제 API 문제가 아니라 풀 용량 부족으로 타임아웃/큐잉이 발생했다는 신호로 볼 수 있음
     * - executor 상태를 조회할 대상 스레드 풀 설정
     */
    private void logExecutorStats(long callId, String when, ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor pool) {
            log.info(
                    "[GEMINI] call={} executor상태({}) active={} poolSize={} queue={} completed={}",
                    callId, when, pool.getActiveCount(), pool.getPoolSize(),
                    pool.getQueue().size(), pool.getCompletedTaskCount());
        }
    }

    @PreDestroy
    public void shutdown() {
        shutdownExecutor(geminiLinkExecutor);
        shutdownExecutor(geminiCurationExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
