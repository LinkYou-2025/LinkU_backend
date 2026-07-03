package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
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

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;

    @Transactional
    public LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        // 1) URL 정규화 & 검증
        String normalizedLink = validateAndNormalizeUrl(dto.getLinku());
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);

        // 2) 기존 Linku 존재 여부 확인-> ai 호출해되 되는지
        Optional<Linku> existingLinku = linkuRepository.findByLinku(normalizedLink);

        Linku linku;
        Category category;
        Optional<LinkuResultDTO> aiResult = Optional.empty();
        String aiTitle = null;
        Long aiEmotionId = null;
        Long aiSituationId = null;
        String keywords = null;
        Domain domain = resolveDomain(domainTail);

        if (existingLinku.isPresent()) { //linkus 테이블에서 가져옴
            linku = existingLinku.get();
            category = linku.getCategory();
            aiTitle = linku.getTitle();
            aiEmotionId = linku.getEmotion().getEmotionId();
            aiSituationId = linku.getSituation().getId();
            keywords = linku.getLinkuKeywords().stream()
                    .map(lk -> lk.getKeyword().getName())
                    .collect(Collectors.joining(", "));

        } else { // ai 요청을 보내 가져옴
            aiResult = geminiLinkuService.analyzeByUrl(normalizedLink, categoryRepository.findAll(), situationRepository.findAll(), emotionRepository.findAll());
            category = resolveCategory(aiResult.map(LinkuResultDTO::categoryId).orElse(null));
            keywords = aiResult.map(LinkuResultDTO::keywords).orElse(null);
            aiEmotionId = aiResult.map(LinkuResultDTO::emotionId).orElse(null);
            aiSituationId = aiResult.map(LinkuResultDTO::situationId).orElse(null);
            aiTitle = aiResult.map(LinkuResultDTO::title).orElse(null);
            String crawledImgUrl = linkToImageService.getRelatedImageFromUrl(normalizedLink, aiTitle);
            Emotion aiEmotion =  emotionRepository.findById(aiEmotionId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
            Situation aiSituation = situationRepository.findById(aiSituationId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
            // 신규 Linku 저장로직
            linku = linkuUpsertService.upsert(normalizedLink, category, domain, aiTitle, crawledImgUrl,aiEmotion,aiSituation);;
            keywordService.saveKeywords(linku, keywords);
        }

        boolean userProvidedEmotion   = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;
        boolean userProvidedTitle     = dto.getTitle() != null && !dto.getTitle().isBlank();

        Emotion emotion     = resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, aiEmotionId);
        Situation situation = resolveSituation(userProvidedSituation ? dto.getSituationId() : null, aiSituationId);
        String userTitle = userProvidedTitle ? dto.getTitle() : aiTitle;


        // 사용자가 보낸 이미지 업로드
        String userImageUrl = uploadUserImage(image);

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

        // 응답 반환
        LinkuResponseDTO.LinkuResultDTO resultDto =
                LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, linku.getDomain(), false, keywords, "");

        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(UrlValidUtils.isURLConnectionOk(normalizedLink))
                .build();
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



    public Domain resolveDomain(String domainTail) {
        return domainTail != null
                ? domainRepository.findByDomainTail(domainTail)
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)))
                : domainRepository.findById(DEFAULT_DOMAIN_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
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
