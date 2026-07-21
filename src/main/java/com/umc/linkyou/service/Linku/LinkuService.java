package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static com.umc.linkyou.converter.LinkuConverter.toLinkuSimpleDTO;

@Service
@RequiredArgsConstructor
public class LinkuService {

    private final LinkuRepository linkuRepository;
    private final CategoryRepository categoryRepository;
    private final EmotionRepository emotionRepository;
    private final SituationRepository situationRepository;
    private final DomainRepository domainRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final FolderRepository folderRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final AiArticleRepository aiArticleRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final LinkuViewService linkuViewService;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;


    @Transactional
    public ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(Long userId, String url) {

        // 1. 정규화 + 영상 링크/유효하지 않은 링크 차단 → 예외 던지기.
        //    LinkuCreateService.validateAndNormalizeUrl()과 동일한 로직(UrlValidUtils.normalizeAndValidateLinkuUrl)을
        //    공유한다. 정규화하지 않은 원본 url로 조회하면, createLinku 시 정규화되어 저장된 값(트레일링 슬래시 제거 등)과
        //    어긋나 이미 저장된 링크인데도 "존재하지 않음"으로 잘못 판정될 수 있다.
        String normalizedUrl = UrlValidUtils.normalizeAndValidateLinkuUrl(url);

        // 3. 기존에 링크 저장 여부 확인
        Optional<UsersLinku> usersLinkuOpt =
                usersLinkuRepository.findByUserIdAndLinku_LinkuUrl(userId, normalizedUrl);

        LinkuResponseDTO.LinkuIsExistDTO dto =
                LinkuConverter.toLinkuIsExistDTO(userId, usersLinkuOpt.orElse(null));

        if (usersLinkuOpt.isPresent()) {
            return ApiResponse.onSuccess(dto);
        } else {
            return ApiResponse.onSuccess(dto);
        }
    }//링크가 이미 존재하는 지 여부 판단



    @Transactional(readOnly = true)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailGetLinku(Long userId, Long linkuId) {
        // 1. 해당 사용자가 이 링크(linkuId)를 저장한 UsersLinku 찾기.
        List<UsersLinku> list = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);

        UsersLinku usersLinku = list.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt)) // 혹은 정렬해서 가장 최근꺼 선택
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        // 2. Linku는 UsersLinku에서 직접 꺼낼 수 있음
        Linku linku = usersLinku.getLinku();

        // 3. 기타 연관 엔티티 처리
        Category category = linku.getCategory();
        Domain domain = linku.getDomain();

        // 4. LinkuFolder 최신 1개 조회
        LinkuFolder linkuFolder =
                linkuFolderRepository.findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()).orElse(null);
        AiArticle aiArticle = aiArticleRepository.findByLinku(linku).orElse(null);
        boolean aiArticleExists = Boolean.TRUE.equals(usersLinku.getAiExist());

        String keyword = null;
        String summary = null;

        String domainName = domain != null ? domain.getName() : null;
        String domainImageUrl = domain != null ? domain.getImageUrl() : null;

        if (aiArticleExists && aiArticle != null) {
            keyword = linku.getLinkuKeywords().stream()
                    .map(lk -> lk.getKeyword().getName())
                    .collect(java.util.stream.Collectors.joining(", "));
            summary = aiArticle.getSummary();
        }

        LinkuResponseDTO.LinkuResultDTO dto = LinkuConverter.toLinkuResultDTO(
                userId, linku, usersLinku, linkuFolder, category, domainName, domainImageUrl,  aiArticleExists, keyword, summary
        );

        //조회수 증가
        linkuViewService.recordView(usersLinku.getUserLinkuId(), linku.getLinkuId());

        return ApiResponse.onSuccess(dto);
    }//링크 상세조회



    @Transactional(readOnly = true)
    public List<LinkuResponseDTO.LinkuSimpleDTO> getRecentViewedLinkus(Long userId, int limit) {
        List<UsersLinku> recentList = usersLinkuRepository
                .findTop10ByUser_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        Map<Long, LinkuFolder> latestFolderByUserLinkuId = fetchLatestLinkuFolders(recentList);

        return recentList.stream()
                .map(ul -> {
                    Linku linku = ul.getLinku();
                    boolean aiArticleExists = Boolean.TRUE.equals(ul.getAiExist());
                    Domain domain = linku.getDomain();
                    LinkuFolder linkuFolder = latestFolderByUserLinkuId.get(ul.getUserLinkuId());
                    return toLinkuSimpleDTO(linku, ul, domain, aiArticleExists, linkuFolder);
                })
                .collect(Collectors.toList());
    }
    //최근 열람한 링크 가져오기  /linku/recent

    // 여러 UsersLinku의 최신 LinkuFolder를 한 번에 조회한다. (리스트 응답에서 N+1 방지)
    // linkuFolderId desc로 조회되므로, 같은 userLinkuId가 여러 번 나와도 먼저 만난(가장 큰 id = 최신) 것을 유지한다.
    private Map<Long, LinkuFolder> fetchLatestLinkuFolders(List<UsersLinku> usersLinkus) {
        if (usersLinkus.isEmpty()) return Map.of();
        List<Long> userLinkuIds = usersLinkus.stream().map(UsersLinku::getUserLinkuId).toList();
        return linkuFolderRepository.findByUsersLinku_UserLinkuIdIn(userLinkuIds).stream()
                .collect(Collectors.toMap(
                        lf -> lf.getUsersLinku().getUserLinkuId(),
                        lf -> lf,
                        (existing, replacement) -> existing
                ));
    }

    //저번 달 저장만 하고 열어보지 않은 링크 가져오기  /linku/last-month/unread
    @Transactional(readOnly = true)
    public List<LinkuResponseDTO.LinkuSimpleDTO> getLastMonthUnreadLinkus(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.plusMonths(1).atDay(1).atStartOfDay();

        return usersLinkuRepository
                .findUnviewedByUserIdAndCreatedAtBetween(userId, start, end)
                .stream()
                .map(LinkuConverter::toLinkuSimpleDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LinkuResponseDTO.LinkuResultDTO updateLinku(Long userId, Long linkuId, LinkuRequestDTO.LinkuUpdateDTO dto) {
        // 1. 본인이 소유한 UsersLinku 찾기 (= 내 userId와 linkuId로 찾음. 못 찾으면 오류)
        List<UsersLinku> list = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);

        UsersLinku usersLinku = list.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))// 혹은 정렬해서 가장 최근꺼 선택
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        // 2. 연관 Linku 엔티티 가져오기 (실제 링크 정보) 및 변경 플래그 준비
        Linku linku = usersLinku.getLinku();
        boolean linkuModified = false;         // Linku 엔티티가 수정됐는지
        boolean usersLinkuModified = false;    // UsersLinku 엔티티가 수정됐는지

        // 3. 메모 변경 (내가 작성한 메모)
        if (dto.getMemo() != null) {
            usersLinku.updateMemo(dto.getMemo());
            usersLinkuModified = true;
        }

        // 4. 감정 아이콘/상태 변경
        if (dto.getEmotionId() != null) {
            Emotion emotion = emotionRepository.findById(dto.getEmotionId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
            usersLinku.updateEmotion(emotion);
            usersLinku.updateEmotionAi(false);
            usersLinkuModified = true;
        }

        // 4-1. 상황 변경
        if (dto.getSituationId() != null) {
            Situation situation = situationRepository.findById(dto.getSituationId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
            usersLinku.updateSituation(situation);
            usersLinku.updateSituationAi(false);
            usersLinkuModified = true;
        }

        // 5. 도메인 변경 (링크의 소속 사이트 교체)
        if (dto.getDomainId() != null) {
            Domain domain = domainRepository.findById(dto.getDomainId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
            linku.updateDomain(domain);
            linkuModified = true;
        }

        // 5-1. 카테고리(중분류) 변경
        //      Linku는 동일 URL을 저장한 모든 유저가 공유하는 엔티티이므로 category 자체를 바꾸지 않고,
        //      이 유저 소유의 해당 카테고리 중분류(루트) 폴더로 LinkuFolder 매핑만 이동한다.
        LinkuFolder linkuFolder = linkuFolderRepository
                .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                .orElse(null);

        if (dto.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new GeneralException(CategoryErrorStatus._CATEGORY_NOT_FOUND));
            Folder targetFolder = usersFolderRepository.findFolderByUserIdAndCategory(userId, newCategory)
                    .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

            if (linkuFolder == null) {
                throw new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND);
            }
            linkuFolder.updateFolder(targetFolder);
            linkuFolderRepository.save(linkuFolder);
        }

        // 6. 제목(title) 변경 (개인화: 공용 Linku가 아닌 이 유저의 UsersLinku.title만 변경)
        if (dto.getTitle() != null) {
            usersLinku.updateTitle(dto.getTitle());
            usersLinkuModified = true;
        }

        // 6-1. 대표 이미지 변경 (개인화: 이 유저의 UsersLinku.imageUrl만 변경)
        //      기존 이미지가 있으면 S3에서 먼저 삭제한 뒤 새 이미지를 업로드한다.
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String newImageUrl = awsS3Service.replaceFile(usersLinku.getImageUrl(), dto.getImage(), "linkucreate");
            usersLinku.updateImageUrl(newImageUrl);
            usersLinkuModified = true;
        }


        // 7. 실제 변경이 발생한 엔티티만 저장(DB update)
        if (linkuModified) linkuRepository.save(linku);
        if (usersLinkuModified) usersLinkuRepository.save(usersLinku);

        // 8. 카테고리, 도메인 등 응답 준비
        //    categoryId는 공유 Linku가 아니라 이 유저가 속한 폴더(중분류) 기준으로 내려준다.
        //    (폴더 매핑이 없는 예외적인 경우에만 공유 Linku의 category로 대체)
        Category category = linkuFolder != null ? linkuFolder.getFolder().getCategory() : linku.getCategory();
        Domain domain = linku.getDomain();

        // 9. DTO 변환해 반환 (모든 정보 최신상태로 응답)
        return LinkuConverter.toLinkuResultDTO(userId, linku, usersLinku, linkuFolder, category, domain, null);
    } //링크 수정 (폴더/카테고리 변경은 updateLinkuFolder로 분리됨)

    /**
     * 링크가 속한 폴더를 변경한다. (폴더 수정 전용 API)
     * 링크 수정(updateLinku)에서 프론트가 실수로 폴더까지 바꿔버리는 것을 막기 위해
     * 폴더 이동은 이 메서드로만 가능하도록 분리했다.
     * Linku는 동일 URL을 저장한 모든 유저가 공유하는 엔티티이므로, 이 메서드는
     * 이 유저 소유의 LinkuFolder 매핑(folder_id)만 바꾸고 linku/category는 건드리지 않는다.
     */
    @Transactional
    public LinkuResponseDTO.LinkuFolderChangeResultDTO updateLinkuFolder(Long userId, Long linkuId, LinkuRequestDTO.LinkuFolderUpdateDTO dto) {
        // 1. 본인이 소유한 UsersLinku 찾기
        List<UsersLinku> list = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
        UsersLinku usersLinku = list.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        Linku linku = usersLinku.getLinku();

        // 2. 이동할 폴더 조회
        Folder folder = folderRepository.findById(dto.getFolderId())
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        // 2-1. 이동할 폴더에 대한 권한 확인 (소유자 또는 편집자만 가능)
        if (!usersFolderRepository.existsFolderOwnerOrWriter(userId, dto.getFolderId())) {
            throw new GeneralException(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN);
        }

        // 3. 현재 링크-폴더 매핑 중 최신 1개를 가져와 폴더 교체 (폴더 이동)
        //    주의: Linku는 동일 URL을 저장한 모든 유저가 공유하는 엔티티이므로
        //    여기서는 절대 linku 자체나 category를 수정하지 않는다.
        //    오직 이 유저 소유의 LinkuFolder 매핑(folder_id)만 변경한다. (user_linku_id는 그대로 유지)
        LinkuFolder linkuFolder = linkuFolderRepository
                .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));
        linkuFolder.updateFolder(folder);
        linkuFolderRepository.save(linkuFolder);

        return LinkuConverter.toLinkuFolderChangeResultDTO(linku, linkuFolder);
    } //링크 폴더 이동



    @Transactional
    public void deleteUsersLinku(Long userId, Long userLinkuId) {
        UsersLinku usersLinku = usersLinkuRepository.findById(userLinkuId)
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        if (!usersLinku.getUser().getId().equals(userId)) {
            throw new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND);
        }

        // 1. linku_folder 관련 삭제
        List<LinkuFolder> linkuFolders = linkuFolderRepository.findByUsersLinku(usersLinku);
        linkuFolderRepository.deleteAll(linkuFolders);

        // 2. curation_linku 관련 삭제
        List<CurationLinku> curationLinkus = curationLinkuRepository.findByUsersLinkuId(userLinkuId);
        curationLinkuRepository.deleteAll(curationLinkus);

        // 3. UsersLinku 삭제
        usersLinkuRepository.delete(usersLinku);
    }
}