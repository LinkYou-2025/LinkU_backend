package com.umc.linkyou.service.Linku;

import com.umc.linkyou.infra.ai.dto.CategoryResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.converter.AiArticleConverter;
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

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;

    @Transactional
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        // 1) URL 정규화 & 검증
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);

        // 2) 기존 Linku 존재 여부 확인 (AI 호출 전 DB 먼저 확인)
        Optional<Linku> existingLinku = linkuRepository.findByLinku(normalizedLink);

        Linku linku;
        Category category;
        String aiKeywords;

        if (existingLinku.isPresent()) {
            // [Case 1] 기존 Linku가 존재하는 경우: AI 호출 생략
            linku = existingLinku.get();
            category = linku.getCategory();
            // 기존 AiArticle이 있다면 해당 키워드를 사용, 없으면 null/기본값 처리
            aiKeywords = (linku.getAiArticle() != null) ? linku.getAiArticle().getKeyword() : "키워드 없음";
        } else {
            // [경쟁 상태 방지 로직]
            // AI 분류 및 도메인 결정 로직은 그대로 유지 (이미 생성된 데이터가 있더라도 분류 결과는 필요할 수 있음)
            CategoryResultDTO aiResult = geminiLinkuService.classifyCategoryByUrl(normalizedLink, categoryRepository.findAll());
            category = resolveCategory(aiResult != null ? aiResult.getCategoryId() : null);
            aiKeywords = (aiResult != null && aiResult.getKeywords() != null) ? aiResult.getKeywords() : "키워드 없음";
            Domain domain = resolveDomain(domainTail);

            try {
                linku = createNewLinku(normalizedLink, category, domain, domainTail);
                createAiArticleIfNeeded(linku, category, resolveEmotion(dto.getEmotionId()), aiKeywords);
            } catch (DataIntegrityViolationException e) {
                linku = linkuRepository.findByLinku(normalizedLink)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._LINKU_NOT_FOUND));
                category = linku.getCategory();
                aiKeywords = (linku.getAiArticle() != null) ? linku.getAiArticle().getKeyword() : aiKeywords;
            }
        }

        // 3) 공통 로직 (사용자 매핑, 이미지 처리 등)
        Emotion emotion = resolveEmotion(dto.getEmotionId());
        Users user = findUser(userId);
        String imageUrl = processImage(image, linku);

        UsersLinku usersLinku = createUsersLinku(user, linku, emotion, dto.getMemo(), imageUrl);
        Folder folder = folderService.findFolder(userId, category);

        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        // 응답 반환
        LinkuResponseDTO.LinkuResultDTO resultDto =
                LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, linku.getDomain(), null, aiKeywords, null);

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
        return linkuRepository.save(LinkuConverter.toLinku(normalizedLink, category, domain, crawledTitle));
    }

    // Utility methods - 모두 public으로 선언

    public String validateAndNormalizeUrl(String url) {
        String normalized = UrlUtils.normalizeUrl(url);
        if (UrlValidUtils.isVideoLink(normalized)) throw new GeneralException(ErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        if (!UrlValidUtils.isValidUrl(normalized)) throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        return normalized;
    }

    private Category resolveCategory(Long aiCategoryId) {
        return Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));
    }


    public Emotion resolveEmotion(Long emotionId) {
        return (emotionId == null || emotionId <= 0)
                ? emotionRepository.findById(DEFAULT_EMOTION_ID).orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND))
                : emotionRepository.findById(emotionId).orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
    }

    public Domain resolveDomain(String domainTail) {
        return domainTail != null
                ? domainRepository.findByDomainTail(domainTail)
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)))
                : domainRepository.findById(DEFAULT_DOMAIN_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
    }

    public void createAiArticleIfNeeded(Linku linku, Category category, Emotion emotion, String aiKeywords) {
        if (aiKeywords == null || aiKeywords.isBlank()) {
            aiKeywords = "키워드 없음";
        }
        if (linku.getAiArticle() == null) {
            Situation defaultSituation = situationRepository.findById(1L)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
            AiArticle aiArticle = AiArticleConverter.toEntityKeywordOnly(aiKeywords, linku, defaultSituation, category, emotion);
            linku.setAiArticle(aiArticle);
            aiArticleRepository.save(aiArticle);
        }
    }

    public Users findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
    }

    public String processImage(MultipartFile image, Linku linku) {
        if (image != null && !image.isEmpty()) {
            return awsS3Service.uploadFile(image, "linkucreate");
        }
        return linkToImageService.getRelatedImageFromUrl(linku.getLinku(), linku.getTitle());
    }

    public UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion, String memo, String imageUrl) {
        UsersLinku usersLinku = LinkuConverter.toUsersLinku(user, linku, emotion, memo, imageUrl);
        return usersLinkuRepository.save(usersLinku);
    }

}