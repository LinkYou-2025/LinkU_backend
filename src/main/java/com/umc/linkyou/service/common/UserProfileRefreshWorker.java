package com.umc.linkyou.service.common;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.recommend.UserProfileRefreshQueue;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.dto.UserKeywordWeightRow;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.recommend.UserContentProfileRepository;
import com.umc.linkyou.repository.recommend.UserProfileKeywordRepository;
import com.umc.linkyou.repository.recommend.UserProfileRefreshQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 홈화면 추천 TextMatch/KeywordMatch용 유저 콘텐츠 프로필({@code user_content_profiles},
 * {@code user_profile_keywords})을 비동기로 재계산하는 워커.
 *
 * 전체 유저를 스캔해서 "누가 바뀌었는지" 추론하지 않고, {@code user_profile_refresh_queue}에
 * 명시적으로 표시된(dirty) 유저만 chunk 단위로 드레인한다. 큐잉은 {@code LinkuCreateService}가
 * 링크 저장 시점에 담당한다. (service/common/README.md 참고)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileRefreshWorker {

    // 한 번 드레인할 때 처리할 최대 유저 수. 너무 크게 잡으면 한 번에 도는 시간이 길어지고,
    // 너무 작게 잡으면 큐가 쌓이는 속도를 못 따라가니 운영하면서 조정 대상.
    private static final int CHUNK_SIZE = 200;
    // 프로필 계산에 반영할 최근 저장 링크 수 캡 — 유저가 링크를 아주 많이 저장해도 이 워커의
    // 1회 작업량이 무한정 커지지 않게 막는다.
    private static final int PROFILE_LINK_LIMIT = 200;
    private static final int TOP_TERM_COUNT = 20;
    // KeywordMatch 서브쿼리 비용을 줄이려고 20 -> 10으로 축소 (UsersLinkuRepositoryImpl#nativeScoreExpression
    // 의 keyword 상관 서브쿼리 참고 — user_profile_keywords 행 수가 늘수록 그 서브쿼리가 스캔할 후보도 늘어난다).
    private static final int TOP_KEYWORD_COUNT = 10;
    // profile_text(trgm fallback 원문) 길이 캡.
    private static final int PROFILE_TEXT_MAX_LENGTH = 4000;
    // Postgres 'simple' tsvector 설정과 맞춰서 토큰화하므로, 한 글자짜리(조사 잔여물 등 노이즈)는 제외한다.
    private static final int MIN_TOKEN_LENGTH = 2;

    private final UserProfileRefreshQueueRepository refreshQueueRepository;
    private final UserContentProfileRepository contentProfileRepository;
    private final UserProfileKeywordRepository profileKeywordRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final LinkuKeywordRepository linkuKeywordRepository;

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void drainQueue() {
        List<UserProfileRefreshQueue> chunk =
                refreshQueueRepository.findAllByOrderByRequestedAtAsc(PageRequest.of(0, CHUNK_SIZE));
        if (chunk.isEmpty()) {
            return;
        }

        log.info("[UserProfileRefresh] {}명 처리 시작", chunk.size());
        int success = 0, failed = 0;
        for (UserProfileRefreshQueue item : chunk) {
            Long userId = item.getUserId();
            try {
                refreshOne(userId);
                // requestedAt까지 일치할 때만 삭제 — 처리 도중 재enqueue된 최신 요청을 지우지 않는다.
                refreshQueueRepository.deleteByUserIdAndRequestedAt(userId, item.getRequestedAt());
                success++;
            } catch (Exception e) {
                // 실패한 유저는 큐에서 지우지 않고 다음 드레인 때 재시도한다.
                log.warn("[UserProfileRefresh] userId={} 처리 실패", userId, e);
                failed++;
            }
        }
        log.info("[UserProfileRefresh] 완료: 성공={}, 실패={}", success, failed);
    }

    // 개별 write(upsertProfile/deleteAllByUserId/upsertWeight)는 각각 리포지토리 메서드에
    // @Transactional @Modifying 으로 걸려 있어 그 자체로 트랜잭션 단위다. 여기(같은 클래스 내
    // self-invocation)에 @Transactional을 걸어도 Spring AOP 프록시를 안 거쳐서 적용되지 않으므로
    // 일부러 붙이지 않았다 — deleteAllByUserId와 upsertWeight 사이 원자성이 깨져도 다음 재계산
    // 주기에서 자연히 복구되는 캐시성 데이터라 허용 가능한 트레이드오프다.
    private void refreshOne(Long userId) {
        List<UsersLinku> links = usersLinkuRepository.findRecentContentForProfile(userId, PROFILE_LINK_LIMIT);
        if (links.isEmpty()) {
            // 저장 링크가 아직 없으면 프로필도 만들지 않는다 — 요청 시점에서 프로필 없음을 0점으로 처리.
            return;
        }

        refreshContentProfile(userId, links);
        refreshKeywordProfile(userId);
    }

    // ---- TextMatch용: title+summary 토큰화해서 상위 단어(tsquery) + 원문(trgm fallback) 저장 ----
    private void refreshContentProfile(Long userId, List<UsersLinku> links) {
        Map<String, Integer> termFrequency = new HashMap<>();
        StringBuilder rawText = new StringBuilder();

        for (UsersLinku ul : links) {
            Linku linku = ul.getLinku();
            String summary = (linku.getAiArticle() != null) ? linku.getAiArticle().getSummary() : null;
            String combined = linku.getTitle() + " " + (summary != null ? summary : "");

            for (String token : tokenize(combined)) {
                termFrequency.merge(token, 1, Integer::sum);
            }
            if (rawText.length() < PROFILE_TEXT_MAX_LENGTH) {
                rawText.append(combined).append(' ');
            }
        }

        String profileTsqueryText = termFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_TERM_COUNT)
                .map(Map.Entry::getKey)
                // to_tsquery 는 '&', '|' 같은 특수문자를 연산자로 해석하므로 순수 토큰만 들어가야 안전하다.
                // tokenize()가 [\p{L}\p{N}]만 남기므로 이 시점의 토큰엔 연산자 문자가 없다.
                .collect(Collectors.joining(" | "));

        String profileText = rawText.length() > PROFILE_TEXT_MAX_LENGTH
                ? rawText.substring(0, PROFILE_TEXT_MAX_LENGTH)
                : rawText.toString();

        contentProfileRepository.upsertProfile(
                userId,
                profileTsqueryText.isBlank() ? null : profileTsqueryText,
                profileText.isBlank() ? null : profileText);
    }

    // ---- KeywordMatch용: 키워드 빈도 상위 K개를 다시 채운다 ----
    private void refreshKeywordProfile(Long userId) {
        profileKeywordRepository.deleteAllByUserId(userId);

        List<UserKeywordWeightRow> topKeywords =
                linkuKeywordRepository.findKeywordFrequencyByUserId(userId, PageRequest.of(0, TOP_KEYWORD_COUNT));
        for (UserKeywordWeightRow row : topKeywords) {
            profileKeywordRepository.upsertWeight(userId, row.keywordId(), row.count().intValue());
        }
    }

    // Postgres to_tsvector('simple', ...) 설정과 거의 같은 수준: 소문자화 + 문자/숫자 이외 기준 분리.
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] rawTokens = text.toLowerCase().split("[^\\p{L}\\p{N}]+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (token.length() >= MIN_TOKEN_LENGTH) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
