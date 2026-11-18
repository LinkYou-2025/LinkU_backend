package com.umc.linkyou.service.Linku;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.umc.linkyou.domain.enums.PageType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

// Prometheus 지표와 보는 목적
// - linku_resolve_total: 단계별 재사용률과 페이지 요청 실패율
// - linku_page_type_total: 새 링크의 페이지 종류 분포
// - linku_create_seconds: 재사용 경로와 새 링크 경로의 p95
// - linku_create_stage_seconds: 병목 단계
@Component
@RequiredArgsConstructor
public class LinkuMetrics {

    private final MeterRegistry meterRegistry;

    public void resolved(ResolveOutcome outcome) {
        meterRegistry.counter("linku.resolve", "outcome", outcome.tag).increment();
    }

    public void pageTypeClassified(PageType pageType) {
        meterRegistry.counter("linku.page_type", "type", pageType.name()).increment();
    }

    // outcome이 null이면 대표 URL 결정 전에 끝난 요청(URL 형식 오류 등)이다.
    public void recordCreate(ResolveOutcome outcome, boolean success, long elapsedNanos) {
        Timer.builder("linku.create")
                .description("링크 저장 API 처리 시간 (대표 URL 결정 단계별)")
                .tag("outcome", outcome == null ? "none" : outcome.tag)
                .tag("result", success ? "success" : "error")
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    public void recordStage(Stage stage, long elapsedNanos) {
        Timer.builder("linku.create.stage")
                .description("링크 저장 API 단계별 처리 시간")
                .tag("stage", stage.tag)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    // 테스트에서 LinkuMetrics를 mock으로 둬도 body가 실행되도록 static으로 둔다.
    public static <T, E extends Exception> T timed(
            LinkuMetrics metrics, Stage stage, StageBody<T, E> body) throws E {
        long start = System.nanoTime();
        try {
            return body.run();
        } finally {
            metrics.recordStage(stage, System.nanoTime() - start);
        }
    }

    public static <E extends Exception> void timedRun(
            LinkuMetrics metrics, Stage stage, StageRunnable<E> body) throws E {
        timed(
                metrics,
                stage,
                () -> {
                    body.run();
                    return null;
                });
    }

    @FunctionalInterface
    public interface StageBody<T, E extends Exception> {
        T run() throws E;
    }

    @FunctionalInterface
    public interface StageRunnable<E extends Exception> {
        void run() throws E;
    }

    // robots처럼 같은 요청에서 여러 번 기록되는 단계가 있다.
    public enum Stage {
        DB_LOOKUP("db_lookup"),
        ROBOTS("robots"),
        FETCH("fetch"), // 페이지 요청 (robots 차단 시 probe 포함)
        IFRAME_FETCH("iframe_fetch"), // 네이버 블로그 본문 iframe
        AI("ai"),
        IMAGE("image"),
        VALIDATE("validate"), // 업로드 전 분류, 폴더 검증
        DOMAIN_REGISTER("domain_register"),
        S3_UPLOAD("s3_upload"),
        PERSIST("persist");

        private final String tag;

        Stage(String tag) {
            this.tag = tag;
        }
    }

    public enum ResolveOutcome {
        // 네트워크 요청 없이 DB에서 재사용
        INPUT_HIT("input_hit"),
        SITE_RULE_HIT("site_rule_hit"),
        ALIAS_HIT("alias_hit"),
        // 페이지 요청 후 재사용
        REDIRECT_HIT("redirect_hit"),
        CANONICAL_HIT("canonical_hit"),
        // AI 분석 중 다른 요청이 같은 대표 URL을 먼저 저장했다
        LATE_HIT("late_hit"),
        // 새 링크 생성. ROBOTS_BLOCKED, FETCH_FAILED는 AI 분석 없이 만든다
        NEW_LINK("new_link"),
        ROBOTS_BLOCKED("robots_blocked"),
        FETCH_FAILED("fetch_failed");

        private final String tag;

        ResolveOutcome(String tag) {
            this.tag = tag;
        }
    }
}
