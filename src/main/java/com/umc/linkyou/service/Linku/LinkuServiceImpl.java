package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awsS3.AwsS3Service;
import com.umc.linkyou.converter.AwsS3Converter;
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
import com.umc.linkyou.googleImgParser.LinkToImageService;
import com.umc.linkyou.openApi.OpenAICategoryClassifier;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.FolderRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.LogRepository.EmotionLogRepository;
import com.umc.linkyou.repository.LogRepository.SituationLogRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.DomainRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.web.dto.linku.LinkuInternalDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.*;
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

    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_FOLDER_ID = 16L;
    private static final Long DEFAULT_DOMAIN_ID = 1L;
    private final SituationCategoryService situationCategoryService;
    private final OpenAICategoryClassifier openAiCategoryClassifier;
    private final FolderConverter folderConverter;

    @Override
    @Transactional
    public LinkuResponseDTO.LinkuResultDTO createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        // URL 정규화 적용
        String normalizedLink = UrlUtils.normalizeUrl(dto.getLinku());

        // 영상 링크 차단
        if (isVideoLink(normalizedLink)) {
            throw new GeneralException(ErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        }

        // 유효하지 않은 링크 차단
        if (!isValidUrl(dto.getLinku())) {
            throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        }

        // AI 카테고리 분류
        Long aiCategoryId = openAiCategoryClassifier.classifyCategoryByUrl(normalizedLink, categoryRepository.findAll());

        Category category = Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));


        Emotion emotion = (dto.getEmotionId() == null || dto.getEmotionId() <= 0)
                ? emotionRepository.findById(DEFAULT_EMOTION_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND))
                : emotionRepository.findById(dto.getEmotionId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
        //도메인 가져오기
        String domainTail = extractDomainTail(normalizedLink);
        Domain domain = (domainTail != null)
                ? domainRepository.findByDomainTail(domainTail)
                .orElseGet(() -> domainRepository.findById(DEFAULT_DOMAIN_ID)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND)))
                : domainRepository.findById(DEFAULT_DOMAIN_ID)
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
        //링크 생성
        Optional<Linku> existingLinkuOpt = linkuRepository.findByLinku(normalizedLink);

        Linku linku = existingLinkuOpt.orElseGet(() -> {
            // 새로 title 크롤링
            String crawledTitle = linkToImageService.extractTitle(normalizedLink);

            // 새로 Linku 생성
            Linku newLinku = Linku.builder()
                    .linku(normalizedLink)
                    .category(category)
                    .domain(domain)
                    .title(crawledTitle)
                    .build();

            return linkuRepository.save(newLinku);
        });

        //요청 보낸 사용자 저장
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        //image저장
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = AwsS3Converter.toImageUrl(image, awsS3Service);
        } else {
            // 링크로 대표 이미지 추출 저장 실패 시 null로 저장
            imageUrl = linkToImageService.getRelatedImageFromUrl(linku.getLinku(),linku.getTitle());
        }

        //usersLinku생성
        UsersLinku usersLinku = LinkuConverter.toUsersLinku(user, linku, emotion, dto.getMemo(),imageUrl);
        usersLinkuRepository.save(usersLinku);

        // 유저가 동일한 폴더명을 가진 폴더를 이미 가지고 있는지 조회
        Folder newfolder = usersFolderRepository.findFolderByUserIdAndFolderName(userId, category.getCategoryName())
                .orElseGet(() -> {
                    Folder newFolder = folderConverter.toFolder(category);
                    folderRepository.save(newFolder);

                    UsersFolder newUsersFolder = folderConverter.toUsersFolder(user, newFolder);
                    usersFolderRepository.save(newUsersFolder);

                    return newFolder;
                });
        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(newfolder, usersLinku);
        linkuFolderRepository.save(linkuFolder);

        return LinkuConverter.toLinkuResultDTO(
                userId, linku, usersLinku, linkuFolder, category,domain
        );
    }
// 링큐 생성

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuIsExistDTO>> existLinku(Long userId, String url) {

        // 1. 영상 링크 차단
        if (isVideoLink(url)) {
            ErrorStatus error = ErrorStatus._LINKU_VIDEO_NOT_ALLOWED;
            return ResponseEntity.status(error.getHttpStatus())
                    .body(ApiResponse.onFailure(error.getCode(), error.getMessage(), null));
        }

        // 2. 유효하지 않은 링크 차단
        if (!isValidUrl(url)) {
            ErrorStatus error = ErrorStatus._LINKU_INVALID_URL;
            return ResponseEntity.status(error.getHttpStatus())
                    .body(ApiResponse.onFailure(error.getCode(), error.getMessage(), null));
        }

        // 3. 기존에 링크 저장 여부 확인
        Optional<UsersLinku> usersLinkuOpt = usersLinkuRepository.findByUserIdAndLinku_Linku(userId, url);

        if (usersLinkuOpt.isPresent()) {
            Linku linku = usersLinkuOpt.get().getLinku();
            LinkuResponseDTO.LinkuIsExistDTO dto = LinkuConverter.toLinkuIsExistDTO(userId, usersLinkuOpt.orElse(null));
            return ResponseEntity.ok(ApiResponse.onSuccess("링큐가 이미 존재합니다.", dto));
        } else {
            LinkuResponseDTO.LinkuIsExistDTO dto = LinkuConverter.toLinkuIsExistDTO(userId, null);
            return ResponseEntity.ok(ApiResponse.onSuccess("링큐가 존재하지 않습니다.", dto));
        }
    }
    //링크가 이미 존재하는 지 여부 판단



    // URL에서 도메인명만 추출 (예: https://blog.naver.com/abc → blog.naver.com)
    public static String extractDomainTail(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            return null;
        }
    }

    //
    private boolean isVideoLink(String url) {
        // 영상 플랫폼 도메인 리스트
        List<String> videoDomains = List.of(
                "youtube.com", "youtu.be", "vimeo.com", "tiktok.com", "dailymotion.com", "kakao.tv", "navertv", "tv.kakao.com"
        );

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            return videoDomains.stream().anyMatch(host::contains);
        } catch (URISyntaxException e) {
            return false;
        }
    }
    private boolean isValidUrl(String url) {
        // 1. URL 형식 문법 검사
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        // 2. HTTP GET 요청으로 실제 존재 여부 검사 (HEAD보다 실패 확률 적음)
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // 3초 타임아웃
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true); // 리다이렉트 허용

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400; // 2xx,3xx 정상 판단
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuResultDTO>> detailGetLinku(Long userId, Long linkuId) {
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

        // 5. DTO 변환 및 반환
        LinkuResponseDTO.LinkuResultDTO dto = LinkuConverter.toLinkuResultDTO(
                userId, linku, usersLinku, linkuFolder, category, domain
        );
        return ResponseEntity.ok(ApiResponse.onSuccess("링크 상세 조회 성공", dto));
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
        List<LinkuResponseDTO.LinkuSimpleDTO> results = new ArrayList<>();

        for (RecentViewedLinku rv : recentList) {
            Linku linku = rv.getLinku();
            UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linku.getLinkuId())
                    .orElse(null);

            Domain domain = linku.getDomain();

            LinkuResponseDTO.LinkuSimpleDTO dto = toLinkuSimpleDTO(linku, usersLinku, domain);
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
        return LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domain);
    } //링크 수정

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>>> recommendLinku(
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
            return ResponseEntity.ok(ApiResponse.onSuccess(Collections.emptyList()));
        }

        List<LinkuInternalDTO.ScoredLinkuDTO> pagedList = scoredList.subList(fromIndex, toIndex);

        List<LinkuResponseDTO.LinkuSimpleDTO> result = pagedList.stream()
                .map(scored -> LinkuConverter.toLinkuSimpleDTO(scored.getUserLinku()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }




}

