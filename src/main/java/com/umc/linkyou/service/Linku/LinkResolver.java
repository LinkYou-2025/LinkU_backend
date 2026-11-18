package com.umc.linkyou.service.Linku;

import static com.umc.linkyou.service.Linku.LinkuMetrics.timed;

import java.util.List;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.infra.parser.NaverBlog;
import com.umc.linkyou.infra.parser.RobotsTxtChecker;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.service.Linku.LinkuMetrics.ResolveOutcome;
import com.umc.linkyou.service.Linku.LinkuMetrics.Stage;
import com.umc.linkyou.utils.CanonicalUrlPolicy;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.utils.UrlValidUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class
LinkResolver {
    private static final String CRAWL_USER_AGENT = "Mozilla/5.0";
    private static final int PAGE_FETCH_TIMEOUT_MS = 5000;

    private final LinkuRepository linkuRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final GeminiLinkuService geminiLinkuService;
    private final LinkToImageService linkToImageService;
    private final CategoryRepository categoryRepository;
    private final SituationRepository situationRepository;
    private final EmotionRepository emotionRepository;
    private final LinkuMetrics linkuMetrics;

    // 이미 저장된 URL이면 네트워크 요청 없이 재사용한다.
    // 방금 열어본 링크를 저장하는 것이라 validUrl은 true로 둔다.
    public LinkResolution resolve(String inputUrl) {
        Optional<LinkResolution> stored =
                timed(linkuMetrics, Stage.DB_LOOKUP, () -> findByInputUrl(inputUrl));
        return stored.isPresent() ? stored.get() : resolveNewLink(inputUrl);
    }

    // DB만 보고 네트워크 요청은 하지 않는다.
    private Optional<LinkResolution> findByInputUrl(String inputUrl) {
        Optional<Linku> byInput = linkuRepository.findByLinku(inputUrl);
        if (byInput.isPresent())
            return Optional.of(stored(ResolveOutcome.INPUT_HIT, byInput.get()));

        String siteRuled = UrlUtils.normalizeUrl(CanonicalUrlPolicy.applySiteRules(inputUrl));
        if (!siteRuled.equals(inputUrl)) {
            Optional<Linku> bySiteRule = linkuRepository.findByLinku(siteRuled);
            if (bySiteRule.isPresent())
                return Optional.of(stored(ResolveOutcome.SITE_RULE_HIT, bySiteRule.get()));
        }

        return usersLinkuRepository
                .findLatestLinkuIdBySavedUrl(inputUrl)
                .flatMap(linkuRepository::findById)
                .map(linku -> stored(ResolveOutcome.ALIAS_HIT, linku));
    }

    private LinkResolution stored(ResolveOutcome outcome, Linku linku) {
        return record(
                outcome, new LinkResolution(true, linku.getLinkuUrl(), Optional.of(linku), null));
    }

    private LinkResolution record(ResolveOutcome outcome, LinkResolution resolution) {
        linkuMetrics.resolved(outcome);
        return resolution.withOutcome(outcome);
    }

    // 대표 URL을 정하고, 새 링크면 크롤링과 AI 분석까지 한다.
    private LinkResolution resolveNewLink(String inputUrl) {
        if (!isCrawlAllowed(inputUrl)) return resolveWithoutCrawling(inputUrl);
        SafeUrlFetcher.FetchedPage page;
        try {
            page =
                    timed(
                            linkuMetrics,
                            Stage.FETCH,
                            () ->
                                    safeUrlFetcher.fetchPage(
                                            inputUrl, CRAWL_USER_AGENT, PAGE_FETCH_TIMEOUT_MS));
        } catch (Exception e) {
            log.info("[링크 저장] 페이지 요청 실패 {}: {}", inputUrl, e.getMessage());
            return record(
                    ResolveOutcome.FETCH_FAILED,
                    new LinkResolution(false, inputUrl, Optional.empty(), null));
        }
        List<SafeUrlFetcher.RedirectHop> hops = page.hops();
        String redirectUrl = CanonicalUrlPolicy.followTrustedRedirects(inputUrl, hops);
        Optional<LinkResolution> reused = reuseIfExists(redirectUrl, inputUrl);
        if (reused.isPresent()) return record(ResolveOutcome.REDIRECT_HIT, reused.get());
        if (!isCrawlAllowed(page.finalUrl()))
            return record(
                    ResolveOutcome.ROBOTS_BLOCKED,
                    new LinkResolution(true, redirectUrl, Optional.empty(), null));
        // 리다이렉트로 도착했거나 로그인 화면이면 canonical이 원래 글 주소가 아니라 쓰지 않는다.
        boolean landedByRedirect = !hops.isEmpty();
        boolean loginForm = CanonicalUrlPolicy.hasPasswordLoginForm(page.document());
        Optional<String> canonicalUrl =
                landedByRedirect || loginForm
                        ? Optional.empty()
                        : CanonicalUrlPolicy.declaredUrl(page.document(), page.finalUrl());
        if (canonicalUrl.isPresent()) {
            reused = reuseIfExists(canonicalUrl.get(), inputUrl);
            if (reused.isPresent()) return record(ResolveOutcome.CANONICAL_HIT, reused.get());
        }
        ContentPage content = toContentPage(page);
        // 바깥 페이지에 canonical이 없으면 iframe 안 문서에서 한 번 더 찾는다.
        if (canonicalUrl.isEmpty()
                && !landedByRedirect
                && !loginForm
                && content.document() != page.document()) {
            canonicalUrl = CanonicalUrlPolicy.declaredUrl(content.document(), page.finalUrl());
            if (canonicalUrl.isPresent()) {
                reused = reuseIfExists(canonicalUrl.get(), inputUrl);
                if (reused.isPresent()) return record(ResolveOutcome.CANONICAL_HIT, reused.get());
            }
        }
        Optional<LinkuResultDTO> aiResult = analyze(content.url(), content.document());
        PageType pageType = aiResult.map(LinkuResultDTO::resolvedPageType).orElse(PageType.UNKNOWN);
        linkuMetrics.pageTypeClassified(pageType);
        String linkUrl = canonicalUrl.orElse(redirectUrl);
        // 비콘텐츠 화면의 분석 결과는 원래 글 기준이 아니라 쓰지 않는다.
        NewLinkuAiData aiData =
                pageType == PageType.LOGIN
                                || pageType == PageType.ERROR
                                || pageType == PageType.PAYWALL
                                || pageType == PageType.OTHER
                        ? null
                        : toAiData(aiResult, linkUrl, content.url(), content.document());
        // 분석하는 동안 다른 요청이 같은 링크를 저장했을 수 있어 다시 확인한다.
        reused = reuseIfExists(linkUrl, inputUrl);
        if (reused.isPresent()) return record(ResolveOutcome.LATE_HIT, reused.get());
        return record(
                ResolveOutcome.NEW_LINK,
                new LinkResolution(
                        true, linkUrl, Optional.empty(), aiData, storablePageType(pageType)));
    }

    // robots.txt가 막으면 접속과 리다이렉트만 확인하고 AI 분석은 하지 않는다.
    private LinkResolution resolveWithoutCrawling(String inputUrl) {
        Optional<SafeUrlFetcher.ProbeResult> probe =
                timed(linkuMetrics, Stage.FETCH, () -> safeUrlFetcher.probe(inputUrl));
        if (probe.isEmpty())
            return record(
                    ResolveOutcome.FETCH_FAILED,
                    new LinkResolution(false, inputUrl, Optional.empty(), null));
        String redirectUrl =
                CanonicalUrlPolicy.followTrustedRedirects(inputUrl, probe.get().hops());
        Optional<LinkResolution> reused = reuseIfExists(redirectUrl, inputUrl);
        if (reused.isPresent()) return record(ResolveOutcome.REDIRECT_HIT, reused.get());
        return record(
                ResolveOutcome.ROBOTS_BLOCKED,
                new LinkResolution(true, redirectUrl, Optional.empty(), null));
    }

    private Optional<LinkResolution> reuseIfExists(String url, String inputUrl) {
        if (url.equals(inputUrl)) return Optional.empty();
        if (UrlValidUtils.isVideoLink(url))
            throw new GeneralException(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        return linkuRepository
                .findByLinku(url)
                .map(linku -> new LinkResolution(true, url, Optional.of(linku), null));
    }

    private boolean isCrawlAllowed(String url) {
        try {
            return timed(
                    linkuMetrics,
                    Stage.ROBOTS,
                    () -> robotsTxtChecker.isAllowed(url, CRAWL_USER_AGENT));
        } catch (Exception e) {
            return false;
        }
    }

    // 네이버 블로그 PC 페이지는 본문이 iframe 안에 있어 여기서 한 번만 받아 둔다.
    private ContentPage toContentPage(SafeUrlFetcher.FetchedPage page) {
        ContentPage outer = new ContentPage(page.finalUrl(), page.document());
        if (!NaverBlog.isBlogUrl(page.finalUrl())) return outer;
        Optional<String> iframeUrl = NaverBlog.postFrameUrl(page.document());
        if (iframeUrl.isEmpty() || !isCrawlAllowed(iframeUrl.get())) return outer;
        try {
            return new ContentPage(
                    iframeUrl.get(),
                    timed(
                            linkuMetrics,
                            Stage.IFRAME_FETCH,
                            () ->
                                    safeUrlFetcher.fetchDocument(
                                            iframeUrl.get(),
                                            CRAWL_USER_AGENT,
                                            PAGE_FETCH_TIMEOUT_MS)));
        } catch (Exception e) {
            log.debug("[크롤링] iframe 조회 실패 {}: {}", iframeUrl.get(), e.getMessage());
            return outer;
        }
    }

    private record ContentPage(String url, Document document) {}

    private Optional<LinkuResultDTO> analyze(String pageUrl, Document doc) {
        try {
            return timed(
                    linkuMetrics,
                    Stage.AI,
                    () ->
                            geminiLinkuService.analyzeByUrl(
                                    pageUrl,
                                    doc,
                                    categoryRepository.findAll(),
                                    situationRepository.findAll(),
                                    emotionRepository.findAll()));
        } catch (Exception e) {
            log.warn("[AI 분석 실패] {}: {}", pageUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private NewLinkuAiData toAiData(
            Optional<LinkuResultDTO> aiResult, String linkUrl, String pageUrl, Document doc) {
        Long aiCategoryId = aiResult.map(LinkuResultDTO::categoryId).orElse(null);
        String keywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
        Long aiEmotionId = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
        Long aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);
        String rawAiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
        String crawledImgUrl =
                timed(
                        linkuMetrics,
                        Stage.IMAGE,
                        () -> linkToImageService.getRelatedImageFromUrl(pageUrl, rawAiTitle, doc));
        String aiTitle =
                LinkuTitleResolver.resolve(
                        rawAiTitle, UrlValidUtils.extractDomainTail(linkUrl), linkUrl);
        return new NewLinkuAiData(
                aiCategoryId, keywords, aiEmotionId, aiSituationId, aiTitle, crawledImgUrl);
    }

    private static PageType storablePageType(PageType pageType) {
        return pageType == PageType.UNKNOWN ? null : pageType;
    }
}
