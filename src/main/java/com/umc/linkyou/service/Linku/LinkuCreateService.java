package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.utils.UrlValidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    private final AiArticleRepository aiArticleRepository;
    private final GeminiLinkuService geminiLinkuService;
    private final FolderService folderService;
    private final KeywordService keywordService;

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;

    @Transactional
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        Users user          = findUser(userId);
        Long jobId = user.getJob().getId();
        // 1) URL 정규화 & 검증
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);

        // 2) 기존 Linku 존재 여부 확인 (AI 호출 전 DB 먼저 확인)
        Optional<Linku> existingLinku = linkuRepository.findByLinku(normalizedLink);

        Linku linku;
        Category category;
        String aiTitle = null;
        Optional<LinkuResultDTO> aiResult = Optional.empty();

        if (existingLinku.isPresent()) {
            linku = existingLinku.get();
            category = linku.getCategory();
            // 기존 링크라도 emotion/situation 중 하나라도 미입력이면 AI 호출
            if (dto.getEmotionId() == null || dto.getSituationId() == null) {
                aiResult = geminiLinkuService.analyzeByUrl(normalizedLink, jobId, categoryRepository.findAll(), situationRepository.findAll(), emotionRepository.findAll());
            }
        } else {
            aiResult = geminiLinkuService.analyzeByUrl(normalizedLink, jobId, categoryRepository.findAll(), situationRepository.findAll(), emotionRepository.findAll());
            category = resolveCategory(aiResult.map(LinkuResultDTO::categoryId).orElse(null));
            String rawKeywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
            aiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
            Domain domain = resolveDomain(domainTail);

            try {
                linku = createNewLinku(normalizedLink, category, domain, domainTail);
                createAiArticleIfNeeded(linku);
                keywordService.saveKeywords(linku, rawKeywords);
            } catch (DataIntegrityViolationException e) {
                linku = linkuRepository.findByLinku(normalizedLink)
                        .orElseThrow(() -> new GeneralException(LinkuErrorStatus._LINKU_NOT_FOUND));
                category = linku.getCategory();
            }
        }

        // 3) emotion/situation: 사용자 입력 우선, 없으면 AI 반환값 사용
        Long aiEmotionId   = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
        Long aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);

        boolean userProvidedEmotion   = dto.getEmotionId() != null;
        boolean userProvidedSituation = dto.getSituationId() != null;

        String userTitle = dto.getTitle() != null ? dto.getTitle() : aiTitle;

        Emotion emotion     = resolveEmotion(dto.getEmotionId(), aiEmotionId);
        Situation situation = resolveSituation(dto.getSituationId(), aiSituationId);

        String userImageUrl = uploadUserImage(image);

        UsersLinku usersLinku = createUsersLinku(
                user, linku, emotion, situation, dto.getMemo(), userImageUrl, userTitle,
                !userProvidedEmotion, !userProvidedSituation
        );
        Folder folder = folderService.findFolder(userId, category);

        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        // 응답 반환
        String keyword = linku.getLinkuKeywords().stream()
                .map(lk -> lk.getKeyword().getName())
                .collect(java.util.stream.Collectors.joining(", "));
        LinkuResponseDTO.LinkuResultDTO resultDto =
                LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, linku.getDomain(), null, keyword, null);

        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(UrlValidUtils.isURLConnectionOk(normalizedLink))
                .build();
    }

    // 신규 생성을 분리한 헬퍼 메서드 (기존 findOrCreateLinku 대체)
    private Linku createNewLinku(String normalizedLink, Category category, Domain domain, String domainTail) {
        String crawledTitle = linkToImageService.extractTitle(normalizedLink);
        if (crawledTitle == null || crawledTitle.isBlank()) {
            crawledTitle = (domainTail != null && !domainTail.isBlank()) ? domainTail : "제목 없음";
        }
        String crawledImgUrl = linkToImageService.getRelatedImageFromUrl(normalizedLink, crawledTitle);
        return linkuRepository.save(LinkuConverter.toLinku(normalizedLink, category, domain, crawledTitle, crawledImgUrl));
    }

    // Utility methods - 모두 public으로 선언

    public String validateAndNormalizeUrl(String url) {
        String normalized = UrlUtils.normalizeUrl(url);
        if (UrlValidUtils.isVideoLink(normalized)) throw new GeneralException(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        if (!UrlValidUtils.isValidUrl(normalized)) throw new GeneralException(LinkuErrorStatus._LINKU_INVALID_URL);
        return normalized;
    }

    private Category resolveCategory(Long aiCategoryId) {
        return Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(CategoryErrorStatus._CATEGORY_NOT_FOUND));
    }


    public Emotion resolveEmotion(Long userEmotionId, Long aiEmotionId) {
        Long resolvedId = (userEmotionId != null && userEmotionId > 0) ? userEmotionId
                        : (aiEmotionId != null)                        ? aiEmotionId
                        : DEFAULT_EMOTION_ID;
        return emotionRepository.findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
    }

    public Domain resolveDomain(String domainTail) {
        return domainTail != null
                ? domainRepository.findByDomainTail(domainTail)
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)))
                : domainRepository.findById(DEFAULT_DOMAIN_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
    }

    public void createAiArticleIfNeeded(Linku linku) {
        if (linku.getAiArticle() == null) {
            AiArticle aiArticle = AiArticle.builder()
                    .linku(linku)
                    .title(linku.getTitle())
                    .summary("")
                    .build();
            linku.setAiArticle(aiArticle);
            aiArticleRepository.save(aiArticle);
        }
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

    public Situation resolveSituation(Long userSituationId, Long aiSituationId) {
        Long resolvedId = (userSituationId != null) ? userSituationId
                        : (aiSituationId != null)   ? aiSituationId
                        : null;
        if (resolvedId == null) return null;
        return situationRepository.findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
    }

    public UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion, Situation situation,
                                       String memo, String imageUrl, String title,
                                       boolean emotionAi, boolean situationAi) {
        UsersLinku usersLinku = LinkuConverter.toUsersLinku(user, linku, emotion, situation, memo, imageUrl, title, emotionAi, situationAi);
        return usersLinkuRepository.save(usersLinku);
    }

}
