package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.NaverBlogIframeFetcher;
import com.umc.linkyou.infra.parser.RobotsTxtChecker;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.common.ImageFetchService;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.service.Linku.LinkuUpsertService;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.recommend.UserProfileRefreshQueueRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.UrlValidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import org.jsoup.nodes.Document;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 링크 저장: URL 정규화 → (신규면) 크롤링·AI 분류·대표 이미지 추출 → 짧은 트랜잭션으로 DB 저장.
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkuCreateService {

    private final LinkuRepository linkuRepository;
    private final CategoryRepository categoryRepository;
    private final EmotionRepository emotionRepository;
    private final DomainRepository domainRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final ImageFetchService imageFetchService;
    private final SituationRepository situationRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final GeminiLinkuService geminiLinkuService;
    private final KeywordService keywordService;
    private final LinkuUpsertService linkuUpsertService;
    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final NaverBlogIframeFetcher naverBlogIframeFetcher;
    private final UserProfileRefreshQueueRepository userProfileRefreshQueueRepository;
    private final TransactionTemplate transactionTemplate;

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;
    private static final Long DEFAULT_SITUATION_ID = 1L;
    private static final int MAX_FALLBACK_TITLE_LENGTH = 50;
    private static final int PAGE_FETCH_TIMEOUT_MS = 15000;
    private static final long IMAGE_WAIT_AFTER_AI_SECONDS = 5;

    // 외부 I/O는 트랜잭션 밖에서, DB 쓰기만 persistLinku 트랜잭션에서 처리한다.
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);
        List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(normalizedLink);

        Optional<Linku> existingLinkuOpt = linkuRepository.findByLinku(normalizedLink);
        boolean isNewLinku = existingLinkuOpt.isEmpty();

        NewLinkuAiData aiData = isNewLinku ? prepareNewLinkuAiData(normalizedLink, domainTail) : null;

        validateBeforeUpload(userId, dto, existingLinkuOpt, aiData);

        String userImageUrl = uploadUserImage(image);
        // 페이지 fetch에 이미 성공했으면 도달 가능한 URL이므로 추가 요청을 생략한다.
        boolean validUrl = (aiData != null && aiData.pageFetched()) || safeUrlFetcher.isReachable(normalizedLink);

        LinkuResponseDTO.LinkuResultDTO resultDto = transactionTemplate.execute(
                (TransactionCallback<LinkuResponseDTO.LinkuResultDTO>) status ->
                        persistLinku(userId, dto, normalizedLink, domainTail, domainTailCandidates, aiData, userImageUrl));

        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(validUrl)
                .build();
    }

    // S3 업로드 전에 감정/상황/폴더 유효성을 미리 검증한다.
    private void validateBeforeUpload(
            Long userId, LinkuRequestDTO.LinkuCreateDTO dto, Optional<Linku> existingLinkuOpt, NewLinkuAiData aiData) {
        boolean userProvidedEmotion = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;

        resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, null);
        resolveSituation(userProvidedSituation ? dto.getSituationId() : null, null);

        Category category = existingLinkuOpt
                .map(Linku::getCategory)
                .orElseGet(() -> resolveCategory(aiData != null ? aiData.aiCategoryId() : null));

        usersFolderRepository.findFolderByUserIdAndCategory(userId, category)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));
    }

    // 페이지를 한 번만 fetch해 AI 분류와 대표 이미지 추출(병렬)에 공유한다. fetch 실패 시 재요청 없이 폴백 제목만 쓴다.
    private NewLinkuAiData prepareNewLinkuAiData(String normalizedLink, String domainTail) {
        Document doc = fetchIfAllowed(normalizedLink, PAGE_FETCH_TIMEOUT_MS);
        if (doc == null) {
            return new NewLinkuAiData(null, null, null, null,
                    resolveTitle(null, domainTail, normalizedLink), null, false);
        }

        NaverBlogIframeFetcher.Result iframePage = naverBlogIframeFetcher.fetchIfPresent(normalizedLink, doc);

        // Jsoup Document는 스레드 안전하지 않아 비동기 이미지 추출에는 복제본을 넘긴다.
        CompletableFuture<String> imageFuture = imageFetchService.fetchAsync(
                normalizedLink, doc.clone(), iframePage.copyForAsyncUse());

        Optional<LinkuResultDTO> aiResult = geminiLinkuService.analyzeByUrl(
                normalizedLink, doc, categoryRepository.findAll(), situationRepository.findAll(),
                emotionRepository.findAll(), iframePage);

        Long aiCategoryId = aiResult.map(LinkuResultDTO::categoryId).orElse(null);
        String keywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
        Long aiEmotionId = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
        Long aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);
        String rawAiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
        String crawledImgUrl = awaitImage(imageFuture);
        String aiTitle = resolveTitle(rawAiTitle, domainTail, normalizedLink);

        return new NewLinkuAiData(aiCategoryId, keywords, aiEmotionId, aiSituationId, aiTitle, crawledImgUrl, true);
    }

    // AI 응답 이후 최대 IMAGE_WAIT_AFTER_AI_SECONDS까지만 이미지 추출을 기다리고, 넘기면 이미지 없이 진행한다.
    private String awaitImage(CompletableFuture<String> imageFuture) {
        try {
            return imageFuture.completeOnTimeout(null, IMAGE_WAIT_AFTER_AI_SECONDS, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            return null;
        }
    }

    // robots.txt 허용 시 페이지를 fetch한다. 차단·실패 시 null.
    private Document fetchIfAllowed(String url, int timeoutMs) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                return null;
            }
            long startedAt = System.nanoTime();
            try {
                Document document = safeUrlFetcher.fetchDocument(url, "Mozilla/5.0", timeoutMs);
                log.info("[링크 생성 시간] 단계=html-fetch elapsedMs={} success=true",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
                return document;
            } catch (Exception e) {
                log.info("[링크 생성 시간] 단계=html-fetch elapsedMs={} success=false",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
                throw e;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // 크롤링·AI 결과를 트랜잭션 단계로 넘기는 값 객체. pageFetched는 페이지 fetch 성공 여부.
    private record NewLinkuAiData(
            Long aiCategoryId, String keywords, Long aiEmotionId, Long aiSituationId,
            String aiTitle, String crawledImgUrl, boolean pageFetched) {}

    // 외부 호출 없이 DB 읽기/쓰기만 수행하는 트랜잭션 본체.
    private LinkuResponseDTO.LinkuResultDTO persistLinku(
            Long userId, LinkuRequestDTO.LinkuCreateDTO dto, String normalizedLink, String domainTail,
            List<String> domainTailCandidates, NewLinkuAiData aiData, String userImageUrl) {

        Domain domain = resolveDomain(domainTailCandidates);
        Optional<Linku> existingLinku = linkuRepository.findByLinku(normalizedLink);

        Linku linku;
        Category category;
        String aiTitle;
        Long aiEmotionId;
        Long aiSituationId;
        String keywords;

        if (existingLinku.isPresent()) {
            linku = existingLinku.get();
            category = linku.getCategory();
            aiTitle = linku.getTitle();
            aiEmotionId = linku.getEmotion().getEmotionId();
            aiSituationId = linku.getSituation().getId();
            keywords = linku.getLinkuKeywords().stream()
                    .map(lk -> lk.getKeyword().getName())
                    .collect(Collectors.joining(", "));

        } else {
            if (aiData == null) {
                aiData = new NewLinkuAiData(null, null, null, null,
                        resolveTitle(null, domainTail, normalizedLink), null, false);
            }
            category = resolveCategory(aiData.aiCategoryId());
            keywords = aiData.keywords();
            aiEmotionId = aiData.aiEmotionId();
            aiSituationId = aiData.aiSituationId();
            aiTitle = aiData.aiTitle();
            Emotion aiEmotion = resolveEmotion(null, aiEmotionId);
            Situation aiSituation = resolveSituation(null, aiSituationId);
            linku = linkuUpsertService.upsert(normalizedLink, category, domain, aiTitle, aiData.crawledImgUrl(), aiEmotion, aiSituation);
            keywordService.saveKeywords(linku, keywords);
        }

        boolean userProvidedEmotion   = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;
        boolean userProvidedTitle     = dto.getTitle() != null && !dto.getTitle().isBlank();

        Emotion emotion     = resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, aiEmotionId);
        Situation situation = resolveSituation(userProvidedSituation ? dto.getSituationId() : null, aiSituationId);
        String userTitle = userProvidedTitle ? dto.getTitle() : aiTitle;

        Users user          = findUser(userId);
        UsersLinku usersLinku = createUsersLinku(
                user, linku, emotion, situation, dto.getMemo(), userImageUrl, userTitle,
                !userProvidedEmotion, !userProvidedSituation
        );

        Folder folder =  usersFolderRepository.findFolderByUserIdAndCategory(userId, category)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        String domainName = domain != null ? domain.getName() : null;
        String domainImageUrl = domain != null ? domain.getImageUrl() : null;

        return LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domainName, domainImageUrl, false, keywords, "");
    }

    public String validateAndNormalizeUrl(String url) {
        return UrlValidUtils.normalizeAndValidateLinkuUrl(url);
    }

    private Category resolveCategory(Long aiCategoryId) {
        return Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(CategoryErrorStatus._CATEGORY_NOT_FOUND));
    }



    // 정확한 호스트 → apex 도메인 → 기본 도메인 순으로 Domain을 찾는다.
    public Domain resolveDomain(List<String> domainTailCandidates) {
        return domainTailCandidates.stream()
                .map(domainRepository::findByDomainTail)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)));
    }


    public Users findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
    }

    public String uploadUserImage(MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            return awsS3Service.uploadFile(image, "linkucreate");
        }
        return null;
    }

    public Emotion resolveEmotion(Long userEmotionId, Long aiEmotionId) {
        Long resolvedId = (userEmotionId != null && userEmotionId > 0) ? userEmotionId
                : (aiEmotionId != null)                        ? aiEmotionId
                : DEFAULT_EMOTION_ID;
        return emotionRepository.findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
    }

    public Situation resolveSituation(Long userSituationId, Long aiSituationId) {
        Long resolvedId = (userSituationId != null && userSituationId > 0) ? userSituationId
                : (aiSituationId != null)                          ? aiSituationId
                : DEFAULT_SITUATION_ID;
        return situationRepository.findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
    }

    // AI 제목 → 도메인+경로 → "제목 없음" 순으로 null이 아닌 제목을 정한다.
    public String resolveTitle(String aiTitle, String domainTail, String url) {
        if (aiTitle != null && !aiTitle.isBlank()) {
            return aiTitle;
        }
        String fallback = buildFallbackTitle(domainTail, url);
        return (fallback != null && !fallback.isBlank()) ? fallback : "제목 없음";
    }

    // 도메인 + 디코딩된 경로를 최대 길이로 잘라 폴백 제목을 만든다.
    private String buildFallbackTitle(String domainTail, String url) {
        if (domainTail == null || domainTail.isBlank()) {
            return null;
        }

        String path = null;
        try {
            path = new URI(url).getPath(); // URI.getPath()는 퍼센트 인코딩을 디코딩된 상태로 반환
        } catch (Exception e) {
            // URL 파싱 실패 시 도메인만 사용
        }

        String combined = (path != null && !path.isBlank() && !path.equals("/"))
                ? domainTail + path
                : domainTail;

        if (combined.length() > MAX_FALLBACK_TITLE_LENGTH) {
            combined = combined.substring(0, MAX_FALLBACK_TITLE_LENGTH) + "…";
        }
        return combined;
    }

    public UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion, Situation situation,
                                       String memo, String imageUrl, String title,
                                       boolean emotionAi, boolean situationAi) {
        UsersLinku usersLinku = LinkuConverter.toUsersLinku(user, linku, emotion, situation, memo, imageUrl, title, emotionAi, situationAi);
        // 같은 링크로 AI 요약을 본 적 있으면 aiExist=true
        boolean userAlreadyHasAiArticle = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(user.getId(), linku.getLinkuId())
                .stream()
                .anyMatch(ul -> Boolean.TRUE.equals(ul.getAiExist()));
        if (userAlreadyHasAiArticle) {
            usersLinku.markAiExist(true);
        }
        UsersLinku saved = usersLinkuRepository.save(usersLinku);

        // 홈 추천용 유저 프로필 재계산 큐에 등록
        userProfileRefreshQueueRepository.enqueue(user.getId());

        return saved;
    }

}
