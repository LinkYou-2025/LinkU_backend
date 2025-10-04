package com.umc.linkyou.service.Linku;

import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awsS3.AwsS3Service;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.converter.FolderConverter;
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
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.openApi.OpenAICategoryClassifier;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkuCreateServiceImpl implements LinkuCreateService {

    private final LinkuRepository linkuRepository;
    private final CategoryRepository categoryRepository;
    private final EmotionRepository emotionRepository;
    private final DomainRepository domainRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final LinkToImageService linkToImageService;
    private final SituationRepository situationRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final AiArticleRepository aiArticleRepository;

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_FOLDER_ID = 16L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;

    private final SituationCategoryService situationCategoryService;
    private final OpenAICategoryClassifier openAiCategoryClassifier;
    private final FolderConverter folderConverter;
    private final AiArticleConverter aiArticleConverter;

    @Override
    @Transactional
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        // 1) URL 정규화 & 검증 (비디오 링크 여부, URL 유효성 체크)
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);

        // 2) AI 분류 실행 → Category + AI 키워드 반환
        AiCategoryInfo aiInfo = resolveCategoryAndKeywords(normalizedLink);
        Category category = aiInfo.category();
        String aiKeywords = aiInfo.aiKeywords();

        // 3) 감정(Emotion) 조회 (없으면 기본값)
        Emotion emotion = resolveEmotion(dto.getEmotionId());

        // 4) Domain 조회 (없으면 기본값)
        Domain domain = resolveDomain(domainTail);

        // 5) Linku 조회 또는 신규 생성
        Linku linku = findOrCreateLinku(normalizedLink, category, domain, domainTail);

        // 6) AI Article 존재 여부 확인하고 필요시 생성
        createAiArticleIfNeeded(linku, category, emotion, aiKeywords);

        // 7) 요청 보낸 사용자 조회
        Users user = findUser(userId);

        // 8) 이미지 저장 (파일 업로드 or 링크 이미지 추출)
        String imageUrl = processImage(image, linku);

        // 9) UsersLinku 생성 & 저장
        UsersLinku usersLinku = createUsersLinku(user, linku, emotion, dto.getMemo(), imageUrl);

        // 9-1) 링크 생성 후 "최근 열람 링크"에도 기록 추가
        updateRecentViewedLinku(userId, linku.getLinkuId());

        // 10) 폴더 조회 또는 신규 생성
        Folder folder = findOrCreateFolder(userId, category);

        // 11) LinkuFolder 생성 & 저장
        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        // 12) 응답 DTO 변환
        LinkuResponseDTO.LinkuResultDTO resultDto =
                LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domain, null, aiKeywords, null);

        // 13) 최종 결과 반환
        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(UrlValidUtils.isURLConnectionOk(normalizedLink)) // URL 연결 가능 여부
                .build();
    }

    // Utility methods - 모두 public으로 선언

    public String validateAndNormalizeUrl(String url) {
        String normalized = UrlUtils.normalizeUrl(url);
        if (UrlValidUtils.isVideoLink(normalized)) throw new GeneralException(ErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        if (!UrlValidUtils.isValidUrl(url)) throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        return normalized;
    }

    public static record AiCategoryInfo(Category category, String aiKeywords) {}

    public AiCategoryInfo resolveCategoryAndKeywords(String normalizedLink) {
        OpenAICategoryClassifier.CategoryResult aiResult =
                openAiCategoryClassifier.classifyCategoryByUrl(normalizedLink, categoryRepository.findAll());
        Long aiCategoryId = (aiResult != null) ? aiResult.getCategoryId() : null;
        String aiKeywords = (aiResult != null) ? aiResult.getKeywords() : null;
        Category category = Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));
        return new AiCategoryInfo(category, aiKeywords);
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

    public Linku findOrCreateLinku(String normalizedLink, Category category, Domain domain, String domainTail) {
        return linkuRepository.findByLinku(normalizedLink)
                .orElseGet(() -> {
                    String crawledTitle = linkToImageService.extractTitle(normalizedLink);
                    if (crawledTitle == null || crawledTitle.isBlank()) {
                        crawledTitle = (domainTail != null && !domainTail.isBlank()) ? domainTail : "제목 없음";
                    }
                    return linkuRepository.save(LinkuConverter.toLinku(normalizedLink, category, domain, crawledTitle));
                });
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
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
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

    public Folder findOrCreateFolder(Long userId, Category category) {
        return usersFolderRepository.findFolderByUserIdAndFolderName(userId, category.getCategoryName())
                .orElseGet(() -> {
                    Folder newFolder = folderConverter.toFolder(category);
                    folderRepository.save(newFolder);
                    UsersFolder newUsersFolder = folderConverter.toUsersFolder(userRepository.getReferenceById(userId), newFolder);
                    usersFolderRepository.save(newUsersFolder);
                    return newFolder;
                });
    }

    // 최근 열람 링크 기록 추가 메서드는 필요에 따라 아래에 구현

    private void updateRecentViewedLinku(Long userId, Long linkuId) {
        // 구현 필요 시 작성
    }
}
