package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awsS3.AwsS3Service;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.converter.FolderConverter;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.converter.LogConverter;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.SituationJob;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.openApi.OpenAICategoryClassifier;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.LogRepository.EmotionLogRepository;
import com.umc.linkyou.repository.LogRepository.SituationLogRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.linku.LinkuInternalDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.umc.linkyou.converter.LinkuConverter.toLinkuSimpleDTO;

@Service
@RequiredArgsConstructor
public class LinkuServiceImpl implements LinkuService {

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
    private final RecentViewedLinkuRepository recentViewedLinkuRepository;
    private final SituationRepository situationRepository;
    private final SituationLogRepository situationLogRepository;
    private final EmotionLogRepository emotionLogRepository;
    private final SituationJobRepository situationJobRepository;
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

        // 2) AI 분류 실행 → Category + AI 키워드 반환
        AiCategoryInfo aiInfo = resolveCategoryAndKeywords(normalizedLink);
        Category category = aiInfo.category;
        String aiKeywords = aiInfo.aiKeywords;

        // 3) 감정(Emotion) 조회 (없으면 기본값)
        Emotion emotion = resolveEmotion(dto.getEmotionId());

        // 4) Domain 조회 (없으면 기본값)
        Domain domain = resolveDomain(normalizedLink);

        // 5) Linku 조회 또는 신규 생성
        Linku linku = findOrCreateLinku(normalizedLink, category, domain);

        // 6) AI Article 존재 여부 확인하고 필요시 생성
        createAiArticleIfNeeded(linku, category, emotion, aiKeywords);

        // 7) 요청 보낸 사용자 조회
        Users user = findUser(userId);

        // 8) 이미지 저장 (파일 업로드 or 링크 이미지 추출)
        String imageUrl = processImage(image, linku);

        // 9) UsersLinku 생성 & 저장
        UsersLinku usersLinku = createUsersLinku(user, linku, emotion, dto.getMemo(), imageUrl);

        // 10) 폴더 조회 또는 신규 생성
        Folder folder = findOrCreateFolder(userId, category);

        // 11) LinkuFolder 생성 & 저장
        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        // 12) 응답 DTO 변환
        LinkuResponseDTO.LinkuResultDTO resultDto =
                LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domain, null);

        // 13) 최종 결과 반환
        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(resultDto)
                .validUrl(UrlValidUtils.isURLConnectionOk(normalizedLink)) // URL 연결 가능 여부
                .build();
    }// 링큐 생성

    //링큐 생성 편의 메소드 시작
    // 1. URL 정규화 및 기본 검증(영상 여부, 유효성)
    private String validateAndNormalizeUrl(String url) {
        String normalized = UrlUtils.normalizeUrl(url);
        if (UrlValidUtils.isVideoLink(normalized)) throw new GeneralException(ErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        if (!UrlValidUtils.isValidUrl(url)) throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        return normalized;
    }

    // 2. AI 카테고리 분류 + Category 조회 결과 묶어서 반환
    private record AiCategoryInfo(Category category, String aiKeywords) {}

    private AiCategoryInfo resolveCategoryAndKeywords(String normalizedLink) {
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
    // 3. 감정(Emotion) 조회
    private Emotion resolveEmotion(Long emotionId) {
        return (emotionId == null || emotionId <= 0)
                ? emotionRepository.findById(DEFAULT_EMOTION_ID).orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND))
                : emotionRepository.findById(emotionId).orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
    }
    // 4. Domain 조회
    private Domain resolveDomain(String normalizedLink) {
        String domainTail = UrlValidUtils.extractDomainTail(normalizedLink);
        return domainTail != null
                ? domainRepository.findByDomainTail(domainTail)
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)))
                : domainRepository.findById(DEFAULT_DOMAIN_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
    }
    // 5. Linku 조회 또는 생성
    private Linku findOrCreateLinku(String normalizedLink, Category category, Domain domain) {
        return linkuRepository.findByLinku(normalizedLink)
                .orElseGet(() -> {
                    String crawledTitle = linkToImageService.extractTitle(normalizedLink);
                    return linkuRepository.save(LinkuConverter.toLinku(normalizedLink, category, domain, crawledTitle));
                });
    }
    // 6. AI Article 생성
    private void createAiArticleIfNeeded(Linku linku, Category category, Emotion emotion, String aiKeywords) {
        if (aiKeywords != null && !aiKeywords.isBlank() && linku.getAiArticle() == null) {
            Situation defaultSituation = situationRepository.findById(1L)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
            AiArticle aiArticle = AiArticleConverter.toEntityKeywordOnly(aiKeywords, linku, defaultSituation, category, emotion);
            linku.setAiArticle(aiArticle);
            aiArticleRepository.save(aiArticle);
        }
    }
    // 7. 사용자 조회
    private Users findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }
    // 8. 이미지 업로드/추출 처리
    private String processImage(MultipartFile image, Linku linku) {
        if (image != null && !image.isEmpty()) {
            return awsS3Service.uploadFile(image, "linkucreate");
        }
        return linkToImageService.getRelatedImageFromUrl(linku.getLinku(), linku.getTitle());
    }
    // 9. UsersLinku 생성/저장
    private UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion, String memo, String imageUrl) {
        UsersLinku usersLinku = LinkuConverter.toUsersLinku(user, linku, emotion, memo, imageUrl);
        return usersLinkuRepository.save(usersLinku);
    }
    // 10. 폴더 조회/생성
    private Folder findOrCreateFolder(Long userId, Category category) {
        return usersFolderRepository.findFolderByUserIdAndFolderName(userId, category.getCategoryName())
                .orElseGet(() -> {
                    Folder newFolder = folderConverter.toFolder(category);
                    folderRepository.save(newFolder);
                    UsersFolder newUsersFolder = folderConverter.toUsersFolder(userRepository.getReferenceById(userId), newFolder);
                    usersFolderRepository.save(newUsersFolder);
                    return newFolder;
                });
    }
    //링큐 생성 편의 메소드 끝

    @Override
    @Transactional
    public ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(Long userId, String url) {

        // 1. 영상 링크 차단 → 예외 던지기
        if (UrlValidUtils.isVideoLink(url)) {
            throw new GeneralException(ErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        }

        // 2. 유효하지 않은 링크 차단 → 예외 던지기
        if (!UrlValidUtils.isValidUrl(url)) {
            throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        }

        // 3. 기존에 링크 저장 여부 확인
        Optional<UsersLinku> usersLinkuOpt =
                usersLinkuRepository.findByUserIdAndLinku_Linku(userId, url);

        LinkuResponseDTO.LinkuIsExistDTO dto =
                LinkuConverter.toLinkuIsExistDTO(userId, usersLinkuOpt.orElse(null));

        if (usersLinkuOpt.isPresent()) {
            return ApiResponse.onSuccess("링큐가 이미 존재합니다.", dto);
        } else {
            return ApiResponse.onSuccess("링큐가 존재하지 않습니다.", dto);
        }
    }//링크가 이미 존재하는 지 여부 판단



    @Override
    @Transactional
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailGetLinku(Long userId, Long linkuId) {
        // 1. 해당 사용자가 이 링크(linkuId)를 저장한 UsersLinku 찾기.
        UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));
        // 최근 열람 기록 upDate
        updateRecentViewedLinku(userId, linkuId);

        // 2. Linku는 UsersLinku에서 직접 꺼낼 수 있음
        Linku linku = usersLinku.getLinku();

        // 3. 기타 연관 엔티티 처리
        Category category = linku.getCategory();
        Emotion emotion = usersLinku.getEmotion();
        Domain domain = linku.getDomain();

        // 4. LinkuFolder 최신 1개 조회
        LinkuFolder linkuFolder =
                linkuFolderRepository.findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()).orElse(null);
        boolean aiArticleExists = aiArticleRepository.existsAiArticleByLinkuId(linkuId);

        // 5. DTO 변환 및 반환
        LinkuResponseDTO.LinkuResultDTO dto = LinkuConverter.toLinkuResultDTO(
                userId, linku, usersLinku, linkuFolder, category, domain, aiArticleExists
        );
        return ApiResponse.onSuccess("링크 상세 조회 성공", dto);
    } //링크 상세조회


    @Transactional
    public void updateRecentViewedLinku(Long userId, Long linkuId) {
// 1. 이미 열람 기록이 있으면 viewedAt만 갱신
        RecentViewedLinku rv = recentViewedLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId)
                .orElse(null);
        if (rv != null) {
            rv.setViewedAt(LocalDateTime.now());
            recentViewedLinkuRepository.save(rv);
            return;
        }

        // 2. 없으면, 기존 데이터 개수 체크 → 10개 이상이면 가장 오래된 것 삭제
        List<RecentViewedLinku> allRecents = recentViewedLinkuRepository
                .findAllByUser_IdOrderByViewedAtDesc(userId); // 이 때 desc/asc 원하는 대로

        if (allRecents.size() >= 10) {
            // 가장 오래된 열람(== viewedAt이 가장 작은/오래된 것) 삭제
            // 만약 OrderByViewedAtDesc라면, 마지막 요소가 가장 오래된 것
            RecentViewedLinku toDelete = allRecents.get(allRecents.size() - 1); // list는 desc로 옴
            recentViewedLinkuRepository.delete(toDelete);
        }

        // 3. insert 새로 생성
        rv = RecentViewedLinku.builder()
                .user(userRepository.getReferenceById(userId))
                .linku(linkuRepository.getReferenceById(linkuId))
                .viewedAt(LocalDateTime.now())
                .build();
        recentViewedLinkuRepository.save(rv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkuResponseDTO.LinkuSimpleDTO> getRecentViewedLinkus(Long userId, int limit) {
        List<RecentViewedLinku> recentList = recentViewedLinkuRepository
                .findTop10ByUser_IdOrderByViewedAtDesc(userId);

        // 모든 linkuId를 한 번에 뽑아서 AI 아티클 존재 여부 조회
        List<Long> linkuIds = recentList.stream()
                .map(rv -> rv.getLinku().getLinkuId())
                .collect(Collectors.toList());

        Map<Long, Boolean> aiArticleExistsMap = aiArticleRepository.existsAiArticleByLinkuIds(linkuIds);

        List<LinkuResponseDTO.LinkuSimpleDTO> results = new ArrayList<>();


        for (RecentViewedLinku rv : recentList) {
            Linku linku = rv.getLinku();
            UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linku.getLinkuId())
                    .orElse(null);
            boolean aiArticleExists = aiArticleExistsMap.getOrDefault(linku.getLinkuId(), false);
            Domain domain = linku.getDomain();

            LinkuResponseDTO.LinkuSimpleDTO dto = toLinkuSimpleDTO(linku, usersLinku, domain, aiArticleExists);
            results.add(dto);
        }
        return results;
    } //최근 열람한 링크 가져오기

    @Override
    @Transactional
    public LinkuResponseDTO.LinkuResultDTO updateLinku(Long userId, Long linkuId, LinkuRequestDTO.LinkuUpdateDTO dto) {
        // 1. 본인이 소유한 UsersLinku 찾기 (= 내 userId와 linkuId로 찾음. 못 찾으면 오류)
        UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));

        // 2. 연관 Linku 엔티티 가져오기 (실제 링크 정보) 및 변경 플래그 준비
        Linku linku = usersLinku.getLinku();
        boolean linkuModified = false;         // Linku 엔티티가 수정됐는지
        boolean usersLinkuModified = false;    // UsersLinku 엔티티가 수정됐는지

        // 3. 폴더 변경(해당 링크를 다른 폴더로 이동)
        if (dto.getFolderId() != null) {
            Folder folder = folderRepository.findById(dto.getFolderId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_NOT_FOUND));
            // 현재 링크-폴더 매핑 중 최신 1개 가져와서 폴더만 새로 세팅 (폴더 이동)
            LinkuFolder linkuFolder = linkuFolderRepository
                    .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                    .orElse(null);
            if (linkuFolder != null) {
                linkuFolder.setFolder(folder);
                linkuFolderRepository.save(linkuFolder);
            }
        }

        // 4. 카테고리 변경 (DTO에 categoryId가 있으면 Linku category 교체)
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));
            linku.setCategory(category);
            linkuModified = true;
        }

        // 5. 링크 주소(URL) 변경
        if (dto.getLinku() != null) {
            linku.setLinku(dto.getLinku());
            linkuModified = true;
        }

        // 6. 메모 변경 (내가 작성한 메모)
        if (dto.getMemo() != null) {
            usersLinku.setMemo(dto.getMemo());
            usersLinkuModified = true;
        }

        // 7. 감정 아이콘/상태 변경
        if (dto.getEmotionId() != null) {
            Emotion emotion = emotionRepository.findById(dto.getEmotionId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
            usersLinku.setEmotion(emotion);
            usersLinkuModified = true;
        }

        // 8. 도메인 변경 (링크의 소속 사이트 교체)
        if (dto.getDomainId() != null) {
            Domain domain = domainRepository.findById(dto.getDomainId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
            linku.setDomain(domain);
            linkuModified = true;
        }

        // 9. 제목(title) 변경
        if (dto.getTitle() != null) {
            linku.setTitle(dto.getTitle());
            linkuModified = true;
        }


        // 11. 실제 변경이 발생한 엔티티만 저장(DB update)
        if (linkuModified) linkuRepository.save(linku);
        if (usersLinkuModified) usersLinkuRepository.save(usersLinku);

        // 12. 최신 폴더 매핑 정보, 카테고리, 도메인 등 다시 조회해 응답 준비
        LinkuFolder linkuFolder = linkuFolderRepository
                .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                .orElse(null);
        Category category = linku.getCategory();
        Domain domain = linku.getDomain();

        // 13. DTO 변환해 반환 (모든 정보 최신상태로 응답)
        return LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domain, null);
    } //링크 수정

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size) {

        List<UsersLinku> userLinkus = usersLinkuRepository.findByUser_Id(userId);

        if (userLinkus.isEmpty())
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NEW_USER);

        if (userLinkus.size() < 3)
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS);

        Emotion selectedEmotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
        Situation selectedSituation = situationRepository.findById(situationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        //situationLog, emotionlog저장
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Long jobId = user.getJob().getId();

        SituationJob situationJob = situationJobRepository.findBySituation_IdAndJob_Id(situationId, jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        Emotion emotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));

        situationLogRepository.save(LogConverter.toSituationLog(user, situationJob));
        emotionLogRepository.save(LogConverter.toEmotionLog(user, emotion));



        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);
        List<LinkuInternalDTO.ScoredLinkuDTO> scoredList = userLinkus.stream()
                .map(linku -> {
                    int emotionScore = EmotionSimilarityUtil.getSimilarityScore(
                            linku.getEmotion().getEmotionId(),
                            selectedEmotion.getEmotionId());

                    Long aiCategoryId = null;
                    if (linku.getLinku() != null && linku.getLinku().getAiArticle() != null) {
                        aiCategoryId = linku.getLinku().getAiArticle().getAiCategoryId();
                    }

                    int situationScore = aiCategoryId == null ? 1 : (mappedCategories.contains(aiCategoryId) ? 2 : 0);

                    int totalScore = emotionScore + situationScore;

                    return LinkuInternalDTO.ScoredLinkuDTO.builder()
                            .userLinku(linku)
                            .emotionScore(emotionScore)
                            .situationScore(situationScore)
                            .totalScore(totalScore)
                            .build();
                })
                .sorted(Comparator.<LinkuInternalDTO.ScoredLinkuDTO>comparingInt(dto -> dto.getTotalScore() == 0 ? Integer.MIN_VALUE : dto.getTotalScore())
                        .reversed()
                        .thenComparing(dto -> dto.getUserLinku().getCreatedAt(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 페이징 처리
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, scoredList.size());

        if (fromIndex > scoredList.size()) {
            return ApiResponse.onSuccess(Collections.emptyList());
        }

        List<LinkuInternalDTO.ScoredLinkuDTO> pagedList = scoredList.subList(fromIndex, toIndex);

        // 1. 추천된 링크들의 linkuId 리스트 추출
        List<Long> linkuIds = pagedList.stream()
                .map(scored -> scored.getUserLinku().getLinku().getLinkuId())
                .collect(Collectors.toList());

        // 2. 한 번의 쿼리로 AI 아티클 존재여부를 Map으로 조회
        Map<Long, Boolean> aiArticleExistsMap = aiArticleRepository.existsAiArticleByLinkuIds(linkuIds);

        // 3. Map을 참고하여 DTO 변환
        List<LinkuResponseDTO.LinkuSimpleDTO> result = pagedList.stream()
                .map(scored -> {
                    UsersLinku userLinku = scored.getUserLinku();
                    Linku linku = userLinku.getLinku();
                    Domain domain = linku.getDomain();

                    // AI 아티클 존재 여부를 Map에서 꺼내옴 (없으면 false)
                    boolean aiArticleExists = aiArticleExistsMap.getOrDefault(linku.getLinkuId(), false);

                    return LinkuConverter.toLinkuSimpleDTO(
                            linku,
                            userLinku,
                            domain,
                            aiArticleExists
                    );
                })
                .collect(Collectors.toList());


        return ApiResponse.onSuccess(result);
    }




}

