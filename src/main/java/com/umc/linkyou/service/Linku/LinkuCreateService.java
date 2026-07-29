package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.LinkToImageService;
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
    private final UserProfileRefreshQueueRepository userProfileRefreshQueueRepository;
    // 크롤링/AI분석/이미지 업로드 등 블로킹 외부 I/O를 @Transactional 메서드 밖에서 수행하기 위해
    // (커넥션을 그 시간만큼 붙들고 있지 않도록) DB 쓰기 구간만 프로그래밍 방식으로 트랜잭션에 넣는다.
    private final TransactionTemplate transactionTemplate;

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;
    private static final Long DEFAULT_SITUATION_ID = 1L;
    private static final int MAX_FALLBACK_TITLE_LENGTH = 50;

    // 이 메서드 자체는 @Transactional이 아니다. 크롤링/AI분석/이미지 업로드/URL 접속확인 같은
    // 블로킹 외부 I/O를 먼저 끝낸 뒤, DB 읽기/쓰기만 짧은 트랜잭션(persistLinku)으로 감싼다.
    // (예전에는 이 메서드 전체가 @Transactional이라, 외부 HTTP 호출이 몰려있는 동안 DB 커넥션이
    //  계속 점유되어 있었다 — 크롤링 대상이 느리거나 트래픽이 몰리면 커넥션 풀 고갈로 이어질 수 있었다.)
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        // 1) URL 정규화 & 검증 (I/O 없음)
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);
        List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(normalizedLink);

        // 2) 신규 링크인지 가볍게 먼저 확인해서, 신규일 때만 크롤링/AI 분석을 수행한다.
        //    (실제 저장 시점에 한 번 더 확인한다 — 아래 3)에서 외부 I/O를 처리하는 동안
        //     다른 요청이 같은 링크를 먼저 저장했을 가능성이 있기 때문)
        Optional<Linku> existingLinkuOpt = linkuRepository.findByLinku(normalizedLink);
        boolean isNewLinku = existingLinkuOpt.isEmpty();

        // 3) 블로킹 외부 I/O는 전부 트랜잭션 밖에서 먼저 끝낸다.
        NewLinkuAiData aiData = isNewLinku ? prepareNewLinkuAiData(normalizedLink, domainTail) : null;

        // 3-1) 이미지를 S3에 올리기 전에 검증부터 한다(카테고리/감정/상황 유효성 + 폴더 존재 여부).
        //      예전에는 이 검증이 persistLinku() 트랜잭션 안, 즉 업로드보다 뒤에 있어서 검증
        //      실패 시 S3에 이미 올라간 이미지가 고아 객체로 남았다. persistLinku()에서 카테고리/
        //      폴더를 다시 조회하는 것과 중복되지만, isNewLinku 사전 확인과 같은 맥락의 안전장치다.
        validateBeforeUpload(userId, dto, existingLinkuOpt, aiData);

        String userImageUrl = uploadUserImage(image);
        boolean validUrl = safeUrlFetcher.isReachable(normalizedLink);

        // 4) DB 읽기/쓰기만 짧은 트랜잭션 안에서 수행한다.
        LinkuResponseDTO.LinkuResultDTO resultDto = transactionTemplate.execute(
                (TransactionCallback<LinkuResponseDTO.LinkuResultDTO>) status ->
                        persistLinku(userId, dto, normalizedLink, domainTail, domainTailCandidates, aiData, userImageUrl));

        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(validUrl)
                .build();
    }

    // 이미지 업로드보다 먼저 실행해서, 여기서 던지는 예외가 S3 고아 객체를 남기지 않게 한다.
    // persistLinku() 트랜잭션 안에서 하는 것과 사실상 같은 검증(카테고리/감정/상황/폴더)을 먼저
    // 한 번 해보는 것 - 그 사이 폴더가 삭제되는 등 극히 드문 경쟁 상황에서는 persistLinku() 쪽
    // 검증이 최종적으로 다시 걸러준다.
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
        Optional<LinkuResultDTO> aiResult = geminiLinkuService.analyzeByUrl(
                normalizedLink, categoryRepository.findAll(), situationRepository.findAll(), emotionRepository.findAll());

        Long aiCategoryId = aiResult.map(LinkuResultDTO::categoryId).orElse(null);
        String keywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
        Long aiEmotionId = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
        Long aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);
        String rawAiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
        String crawledImgUrl = linkToImageService.getRelatedImageFromUrl(normalizedLink, rawAiTitle);
        // 크롤링/AI 둘 다 실패해도 title이 null로 안 나가도록 보장 (linkus.title은 NOT NULL)
        String aiTitle = resolveTitle(rawAiTitle, domainTail, normalizedLink);

        return new NewLinkuAiData(aiCategoryId, keywords, aiEmotionId, aiSituationId, aiTitle, crawledImgUrl);
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

        if (existingLinku.isPresent()) { //linkus 테이블에서 가져옴
            linku = existingLinku.get();
            category = linku.getCategory();
            aiTitle = linku.getTitle();
            aiEmotionId = linku.getEmotion().getEmotionId();
            aiSituationId = linku.getSituation().getId();
            keywords = linku.getLinkuKeywords().stream()
                    .map(lk -> lk.getKeyword().getName())
                    .collect(Collectors.joining(", "));

        } else { // 신규 저장 - aiData는 createLinku()에서 이미 외부 I/O로 준비해 온 값이다.
            // 극히 드문 경쟁 상황 방어: createLinku()가 "기존 링크 있음"으로 판단해 aiData를
            // 준비하지 않았는데(=null), 그 사이 해당 링크가 삭제되어 여기서는 신규로 보이는 경우.
            // 이때는 AI 분석을 다시 트랜잭션 안에서 수행할 수 없으므로(블로킹 I/O 금지) 폴백 제목만으로 진행한다.
            if (aiData == null) {
                aiData = new NewLinkuAiData(null, null, null, null,
                        resolveTitle(null, domainTail, normalizedLink), null);
            }
            category = resolveCategory(aiData.aiCategoryId());
            keywords = aiData.keywords();
            aiEmotionId = aiData.aiEmotionId();
            aiSituationId = aiData.aiSituationId();
            aiTitle = aiData.aiTitle();
            Emotion aiEmotion = resolveEmotion(null, aiEmotionId); //null이면 기본값으로 대체됨
            Situation aiSituation = resolveSituation(null, aiSituationId);
            // 신규 Linku 저장로직
            linku = linkuUpsertService.upsert(normalizedLink, category, domain, aiTitle, aiData.crawledImgUrl(), aiEmotion, aiSituation);
            keywordService.saveKeywords(linku, keywords);
        }

        boolean userProvidedEmotion   = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;
        boolean userProvidedTitle     = dto.getTitle() != null && !dto.getTitle().isBlank();

        Emotion emotion     = resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, aiEmotionId);
        Situation situation = resolveSituation(userProvidedSituation ? dto.getSituationId() : null, aiSituationId);
        String userTitle = userProvidedTitle ? dto.getTitle() : aiTitle;

        // useslinku 처리
        Users user          = findUser(userId);
        UsersLinku usersLinku = createUsersLinku(
                user, linku, emotion, situation, dto.getMemo(), userImageUrl, userTitle,
                !userProvidedEmotion, !userProvidedSituation
        );

        // 따로 FolderService로 분리하기에는 따로 생성이 되지 않음
        Folder folder =  usersFolderRepository.findFolderByUserIdAndCategory(userId, category)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        String domainName = domain != null ? domain.getName() : null;
        String domainImageUrl = domain != null ? domain.getImageUrl() : null;

        return LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domainName, domainImageUrl, false, keywords, "");
    }

    // Utility methods - 모두 public으로 선언

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
        // "본인"이 과거에 이 링크(linku)를 저장하면서 AI 요약을 직접 요청/조회한 적이 있다면, 이번에
        // 새로 생기는 저장 건도 처음부터 "AI 요약 있음"으로 표시한다 (동일 유저가 같은 링크를 여러 번
        // 저장해도 이미 자신이 확인한 요약이 "요약 없음"으로 보이면 안 되기 때문).
        // 단, 다른 유저가 먼저 이 링크를 요약해뒀을 뿐 본인은 요청/조회한 적이 없다면 true로 표시하지 않는다.
        boolean userAlreadyHasAiArticle = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(user.getId(), linku.getLinkuId())
                .stream()
                .anyMatch(ul -> Boolean.TRUE.equals(ul.getAiExist()));
        if (userAlreadyHasAiArticle) {
            usersLinku.markAiExist(true);
        }
        UsersLinku saved = usersLinkuRepository.save(usersLinku);

        // 홈화면 추천 TextMatch/KeywordMatch용 유저 콘텐츠 프로필이 이 유저의 저장 링크 목록 변경을
        // 반영하도록 재계산 대상으로 표시해둔다. 전체 유저 스캔 없이 UserProfileRefreshWorker가
        // 이 큐만 chunk 단위로 드레인한다 (service/common/README.md 참고).
        userProfileRefreshQueueRepository.enqueue(user.getId());

        return saved;
    }

}
