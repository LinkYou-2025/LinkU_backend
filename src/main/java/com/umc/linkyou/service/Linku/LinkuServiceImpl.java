package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.curationLinkuRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final RecentViewedLinkuRepository recentViewedLinkuRepository;
    private final AiArticleRepository aiArticleRepository;
    private final CurationLinkuRepository curationLinkuRepository;


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
        List<UsersLinku> list = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);

        UsersLinku usersLinku = list.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt)) // 혹은 정렬해서 가장 최근꺼 선택
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
        AiArticle aiArticle = aiArticleRepository.findByLinku(linku).orElse(null);
        boolean aiArticleExists = Boolean.TRUE.equals(usersLinku.getAiExist());

        String keyword = null;
        String summary = null;

        if (aiArticleExists) {
            keyword = aiArticle.getKeyword();
            summary = aiArticle.getSummary();
        }

        LinkuResponseDTO.LinkuResultDTO dto = LinkuConverter.toLinkuResultDTO(
                userId, linku, usersLinku, linkuFolder, category, domain, aiArticleExists, keyword, summary
        );

        return ApiResponse.onSuccess("링크 상세 조회 성공", dto);
    }//링크 상세조회


    @Transactional
    public void updateRecentViewedLinku(Long userId, Long linkuId) {
        // [수정됨] 기존의 10개 제한 로직 및 시간 덮어쓰기 로직 삭제
        // 유저가 링크를 열람할 때마다 무조건 새로운 기록(Log)을 쌓도록 변경 (Append-only)
        RecentViewedLinku rv = RecentViewedLinku.builder()
                .user(userRepository.getReferenceById(userId))
                .linku(linkuRepository.getReferenceById(linkuId))
                .viewedAt(LocalDateTime.now())
                .build();

        recentViewedLinkuRepository.save(rv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkuResponseDTO.LinkuSimpleDTO> getRecentViewedLinkus(Long userId, int limit) {
        // [수정됨] 로그가 누적되므로, 같은 링크를 여러 번 봤더라도 홈 화면에는 중복 없이 최근 순으로 가져와야 함
        // (Repository에 새로 추가할 findDistinctLinkuIdsByUserId 쿼리 사용)
        List<Long> linkuIds = recentViewedLinkuRepository.findDistinctLinkuIdsByUserId(userId, org.springframework.data.domain.PageRequest.of(0, limit));

        if (linkuIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<LinkuResponseDTO.LinkuSimpleDTO> results = new ArrayList<>();

        for (Long linkuId : linkuIds) {
            // 1. Linku 가져오기
            Linku linku = linkuRepository.findById(linkuId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));

            // 2. UsersLinku 찾기
            UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId).stream()
                    .max(Comparator.comparing(UsersLinku::getCreatedAt))
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));

            // 3. AI 요약 여부 및 도메인
            boolean aiArticleExists = Boolean.TRUE.equals(usersLinku.getAiExist());
            Domain domain = linku.getDomain();

            // 4. DTO 변환 및 결과 리스트 추가
            LinkuResponseDTO.LinkuSimpleDTO dto = toLinkuSimpleDTO(linku, usersLinku, domain, aiArticleExists);
            results.add(dto);
        }
        return results;
    }
    //최근 열람한 링크 가져오기

    @Override
    @Transactional
    public LinkuResponseDTO.LinkuResultDTO updateLinku(Long userId, Long linkuId, LinkuRequestDTO.LinkuUpdateDTO dto) {
        // 1. 본인이 소유한 UsersLinku 찾기 (= 내 userId와 linkuId로 찾음. 못 찾으면 오류)
        List<UsersLinku> list = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);

        UsersLinku usersLinku = list.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))// 혹은 정렬해서 가장 최근꺼 선택
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



    @Transactional
    public void deleteUsersLinku(Long userId, Long userLinkuId) {
        UsersLinku usersLinku = usersLinkuRepository.findById(userLinkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));

        if (!usersLinku.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND);
        }

        // 1. linku_folder 관련 삭제
        List<LinkuFolder> linkuFolders = linkuFolderRepository.findByUsersLinku(usersLinku);
        linkuFolderRepository.deleteAll(linkuFolders);

        // 2. curation_linku 관련 삭제
        List<CurationLinku> curationLinkus = curationLinkuRepository.findByUsersLinku_UserLinkuId(userLinkuId);
        curationLinkuRepository.deleteAll(curationLinkus);

        // 3. 최근 열람 기록 삭제 - 이 부분에 linkuId 필요
        Long linkuId = usersLinku.getLinku().getLinkuId();
        recentViewedLinkuRepository.deleteByUser_IdAndLinku_LinkuId(userId, linkuId);

        // 4. UsersLinku 삭제
        usersLinkuRepository.delete(usersLinku);
    }




}

