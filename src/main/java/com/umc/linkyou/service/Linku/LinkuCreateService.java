package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.infra.parser.RobotsTxtChecker;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkuCreateService {

    private final LinkuRepository linkuRepository;
    private final CategoryRepository categoryRepository;
    private final EmotionRepository emotionRepository;
    private final DomainRepository domainRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final LinkToImageService linkToImageService;
    private final SituationRepository situationRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final GeminiLinkuService geminiLinkuService;
    private final KeywordService keywordService;
    private final LinkuUpsertService linkuUpsertService;
    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final UserProfileRefreshQueueRepository userProfileRefreshQueueRepository;
    private final TransactionTemplate transactionTemplate;

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;
    private static final Long DEFAULT_SITUATION_ID = 1L;
    private static final int MAX_FALLBACK_TITLE_LENGTH = 50;

    // 외부 I/O(크롤링/AI분석/업로드/URL확인)는 트랜잭션 밖에서 처리하고, DB 처리만 짧은 트랜잭션(persistLinku)으로 감싼다.
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);
        List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(normalizedLink);

        // 신규 링크일 때만 크롤링/AI 분석 수행
        Optional<Linku> existingLinkuOpt = linkuRepository.findByLinku(normalizedLink);
        boolean isNewLinku = existingLinkuOpt.isEmpty();

        NewLinkuAiData aiData = isNewLinku ? prepareNewLinkuAiData(normalizedLink, domainTail) : null;

        // S3 업로드 전 카테고리/감정/상황/폴더 유효성 검증
        validateBeforeUpload(userId, dto, existingLinkuOpt, aiData);

        String userImageUrl = uploadUserImage(image);
        boolean validUrl = safeUrlFetcher.isReachable(normalizedLink);

        LinkuResponseDTO.LinkuResultDTO resultDto = transactionTemplate.execute(
                (TransactionCallback<LinkuResponseDTO.LinkuResultDTO>) status ->
                        persistLinku(userId, dto, normalizedLink, domainTail, domainTailCandidates, aiData, userImageUrl));

        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(validUrl)
                .build();
    }

    // S3 업로드 전에 카테고리/감정/상황/폴더 유효성을 검증한다 (persistLinku와 동일 로직).
    private void validateBeforeUpload(
            Long userId, LinkuRequestDTO.LinkuCreateDTO dto, Optional<Linku> existingLinkuOpt, NewLinkuAiData aiData) {
        boolean userProvidedEmotion = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;

        // 사용자가 emotionId/situationId를 직접 지정했다면 존재하는 값인지 미리 확인한다.
        resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, null);
        resolveSituation(userProvidedSituation ? dto.getSituationId() : null, null);

        // 기존 링크면 이미 저장된 카테고리를, 신규 링크면 AI가 분류한(혹은 기본) 카테고리를 기준으로
        // 이 유저의 해당 카테고리 폴더가 있는지 확인한다. persistLinku()의 분기와 동일한 로직이다.
        Category category = existingLinkuOpt
                .map(Linku::getCategory)
                .orElseGet(() -> resolveCategory(aiData != null ? aiData.aiCategoryId() : null));

        usersFolderRepository.findFolderByUserIdAndCategory(userId, category)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));
    }

    // 크롤링(제목/본문/이미지) + AI 분석까지, DB에 쓰지 않는 순수 외부 I/O 단계.
    private NewLinkuAiData prepareNewLinkuAiData(String normalizedLink, String domainTail) {
        // 같은 URL을 여기서 한 번만 fetch해 아래 호출들에 공유한다 (중복 fetch 방지)
        Document doc = fetchIfAllowed(normalizedLink, 15000);

        Optional<LinkuResultDTO> aiResult = geminiLinkuService.analyzeByUrl(
                normalizedLink, doc, categoryRepository.findAll(), situationRepository.findAll(), emotionRepository.findAll());

        Long aiCategoryId = aiResult.map(LinkuResultDTO::categoryId).orElse(null);
        String keywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
        Long aiEmotionId = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
        Long aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);
        String rawAiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
        String crawledImgUrl = linkToImageService.getRelatedImageFromUrl(normalizedLink, rawAiTitle, doc);
        // 크롤링/AI 둘 다 실패해도 title이 null로 안 나가도록 보장 (linkus.title은 NOT NULL)
        String aiTitle = resolveTitle(rawAiTitle, domainTail, normalizedLink);

        return new NewLinkuAiData(aiCategoryId, keywords, aiEmotionId, aiSituationId, aiTitle, crawledImgUrl);
    }

    // 실패/차단 시 null 반환 → 소비자들이 각자 직접 fetch하는 기존 경로로 폴백
    private Document fetchIfAllowed(String url, int timeoutMs) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                return null;
            }
            return safeUrlFetcher.fetchDocument(url, "Mozilla/5.0", timeoutMs);
        } catch (Exception e) {
            return null;
        }
    }

    // prepareNewLinkuAiData()의 결과를 트랜잭션 단계로 넘기기 위한 값 객체.
    private record NewLinkuAiData(
            Long aiCategoryId, String keywords, Long aiEmotionId, Long aiSituationId,
            String aiTitle, String crawledImgUrl) {}

    // DB 읽기/쓰기만 수행하는 트랜잭션 본체. 외부 HTTP 호출을 하지 않는다.
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
            // aiData가 없으면(=null) 폴백 제목만으로 진행
            if (aiData == null) {
                aiData = new NewLinkuAiData(null, null, null, null,
                        resolveTitle(null, domainTail, normalizedLink), null);
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



    // domainTailCandidates: [정확한 호스트, (있다면) registry-suffix apex 도메인] 순서.
    // someuser.tistory.com처럼 정확히 일치하는 domains 행이 없으면 apex(tistory.com) 행으로 폴백한다.
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

    // 크롤링/AI 둘 다 실패해도 절대 null을 리턴하지 않음 (linkus.title NOT NULL 대응)
    public String resolveTitle(String aiTitle, String domainTail, String url) {
        if (aiTitle != null && !aiTitle.isBlank()) {
            return aiTitle;
        }
        String fallback = buildFallbackTitle(domainTail, url);
        return (fallback != null && !fallback.isBlank()) ? fallback : "제목 없음";
    }

    // 도메인 + URL 디코딩된 경로 조합, 길이 제한
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
        // 이 유저가 과거 같은 링크로 AI 요약을 본 적 있으면 aiExist=true로 표시
        boolean userAlreadyHasAiArticle = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(user.getId(), linku.getLinkuId())
                .stream()
                .anyMatch(ul -> Boolean.TRUE.equals(ul.getAiExist()));
        if (userAlreadyHasAiArticle) {
            usersLinku.markAiExist(true);
        }
        UsersLinku saved = usersLinkuRepository.save(usersLinku);

        // 홈 추천용 유저 프로필 재계산 큐에 등록 (service/common/README.md 참고)
        userProfileRefreshQueueRepository.enqueue(user.getId());

        return saved;
    }

}
