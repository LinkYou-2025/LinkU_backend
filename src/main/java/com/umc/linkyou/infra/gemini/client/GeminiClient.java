package com.umc.linkyou.infra.gemini.client;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.ai.AiClient;
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

    // 실제 Gemini 호출에 타임아웃을 강제하기 위한 전용 executor
    private final ExecutorService geminiCallExecutor = Executors.newFixedThreadPool(10);
    private final AtomicLong callSeq = new AtomicLong();

    @Value("${gemini.model.name}")
    private String modelName;

    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

    @Override
    public String completion(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.3f, GEMINI_TIMEOUT_SECONDS);
    }

    // 창의적인 응답 생성
    @Override
    public String completionCreative(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.9f, GEMINI_TIMEOUT_SECONDS);
    }

    // Google Search로 실시간 정보 반영
    @Override
    public String completionWithSearch(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, GOOGLE_SEARCH_TOOL, 2048, 0.3f, GEMINI_SEARCH_TIMEOUT_SECONDS);
    }

    private String generate(
            String systemInstruction, String userPrompt, Tool tool, int maxTokens, float temp, int timeoutSeconds)
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
        logExecutorStats(callId, "제출 전");

        // 타임아웃 없이 동기 블로킹이라 응답이 오지 않으면 세마포어 영구고갈 가능 -> 별도 스레드로 던지고 정해진 시간만 대기 후 포기하도록 강제
        Future<String> future = geminiCallExecutor.submit(
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
            logExecutorStats(callId, "타임아웃 직후");
            throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
        } catch (Exception e) {
            log.error(
                    "[GEMINI] call={} 호출 오류, elapsed={}ms",
                    callId, System.currentTimeMillis() - submittedAt, e);
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }

    private void logExecutorStats(long callId, String when) {
        if (geminiCallExecutor instanceof ThreadPoolExecutor pool) {
            log.info(
                    "[GEMINI] call={} executor상태({}) active={} poolSize={} queue={} completed={}",
                    callId, when, pool.getActiveCount(), pool.getPoolSize(),
                    pool.getQueue().size(), pool.getCompletedTaskCount());
        }
    }

    @PreDestroy
    public void shutdown() {
        geminiCallExecutor.shutdown();
        try {
            if (!geminiCallExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                geminiCallExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            geminiCallExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
