package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "linku-controller", description = "링크(Linku) 관련 API")
@RequestMapping("/linku")
public interface LinkuApi {

    @Operation(
            summary = "링크 생성",
            description = """
                    새로운 링크를 생성합니다.

                    - **emotionId** (선택): 감정 ID. 미입력 시 AI 분류값이 사용됩니다.
                    - **situationId** (선택): 상황 ID. 미입력 시 AI 분류값이 사용됩니다. 입력 시 사용자의 직업(job)에 해당하는 상황만 선택 가능합니다.
                      - job_id 1 → situation 1~8
                      - job_id 2 → situation 9~16
                      - job_id 3 → situation 17~24
                      - job_id 4 → situation 25~32
                      - job_id 5 → situation 33~40
                      - job_id 6 → situation 41~48
                    - **title** (선택): 미입력 시 AI 분석값이 사용됩니다.
                    - **image** (선택): 대표 이미지. 미첨부 시 URL에서 자동 추출합니다.
                    """
    )
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._LINKU_INVALID_URL, LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED, LinkuErrorStatus._KEYWORD_NOT_FOUND, LinkuErrorStatus._SITUATION_NOT_MATCH_JOB,LinkuErrorStatus._LINKU_CONFLICT})
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String linku,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) Long situationId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) MultipartFile image
    );

    @Operation(summary = "링크 존재 여부 확인", description = "해당 URL의 링크가 이미 존재하는지 확인합니다.")
    @GetMapping("/exist")
    ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String url
    );

    @Operation(summary = "링크 상세 조회 (인증된 사용자)", description = "인증된 사용자의 링크 상세 정보를 조회합니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._LINKU_NOT_FOUND, LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @GetMapping("/{linkuid}")
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("linkuid") Long linkuid
    );

    @Operation(summary = "링크 상세 조회 (사용자 ID 지정)", description = "특정 사용자 ID와 링크 ID로 링크 상세 정보를 조회합니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._LINKU_NOT_FOUND, LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @GetMapping("/{userId}/{linkuId}")
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @PathVariable Long userId,
            @PathVariable Long linkuId
    );

    @Operation(summary = "최근 열람한 링크 조회", description = "사용자가 최근에 열람한 링크 목록을 조회합니다. limit 파라미터로 조회 개수를 지정할 수 있습니다.")
    @GetMapping("/recent")
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getRecentViewedLinkus(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit
    );

    @Operation(
            summary = "링크 수정",
            description = """
                    기존 링크의 정보(메모, 감정, 상황, 도메인, 카테고리, 제목, 대표 이미지)를 수정합니다. 모든 필드는 선택이며, 보낸 필드만 변경됩니다.

                    - **image** (선택): 새 이미지를 첨부하면 기존에 등록돼 있던 이미지(있는 경우)는 S3에서 삭제되고 새 이미지로 교체됩니다. 첨부하지 않으면 기존 이미지가 그대로 유지됩니다.
                    - **categoryId** (선택): 카테고리를 변경하면 링크(Linku)의 공유 카테고리 자체는 바뀌지 않고, 내 폴더 중 해당 카테고리의 중분류(루트) 폴더로 이 링크가 이동합니다. 소분류로는 이동하지 않습니다.
                    - URL 자체는 이 API로 변경할 수 없습니다.
                    - 소분류 폴더로의 이동은 이 API로 처리하지 않고 별도의 링크 폴더 이동 API(`PATCH /linku/{linkuId}/folder`)를 사용해야 합니다.
                    """
    )
    @ApiErrorCode(
            linkuErrorStatus = {LinkuErrorStatus._LINKU_NOT_FOUND, LinkuErrorStatus._USER_LINKU_NOT_FOUND},
            errorStatus = {ErrorStatus._DOMAIN_NOT_FOUND},
            categoryErrorStatus = {CategoryErrorStatus._CATEGORY_NOT_FOUND},
            folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}
    )
    @PatchMapping(value = "/{linkuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) Long situationId,
            @RequestParam(required = false) Long domainId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) MultipartFile image
    );

    @Operation(summary = "링크 폴더 이동", description = "링크가 속한 폴더를 변경합니다. 링크(Linku)는 동일 URL을 저장한 모든 유저가 공유하는 데이터이므로, 이 API는 해당 유저 소유의 폴더 매핑만 변경하며 링크 자체의 카테고리는 변경하지 않습니다. 이동 대상 폴더는 중분류(최상위 폴더)만 지정할 수 있습니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND}, folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN})
    @PatchMapping(value = "/{linkuId}/folder", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuFolderChangeResultDTO> updateLinkuFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @Valid @RequestBody LinkuRequestDTO.LinkuFolderUpdateDTO updateDTO
    );

    @Operation(summary = "링크 추천", description = "상황(situation)과 감정(emotion)을 기반으로 링크를 추천합니다. 페이지네이션을 지원합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._SITUATION_NOT_FOUND, ErrorStatus._EMOTION_NOT_FOUND, ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS, ErrorStatus._RECOMMEND_LINKU_NO_RECOMMENDATION, ErrorStatus._RECOMMEND_LINKU_NEW_USER})
    @GetMapping("/recommend")
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam Long situationId,
            @RequestParam Long emotionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );

    @Operation(summary = "링크 검색", description = "사용자가 저장한 링크에서 제목·태그가 키워드와 일치하는 링크 목록을 최신 저장 순으로 조회합니다. 커서는 필수입니다. 첫 페이지는 0, 이후에는 응답의 nextCursor 값을 보냅니다.")
    @GetMapping("/search")
    ApiResponse<LinkuSearchResponseDTO.LinkuSearchCursorPageResponse> searchLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") @PositiveOrZero Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    );

    @Operation(summary = "검색어 자동완성", description = "사용자가 저장한 링크의 제목에서 키워드와 일치하는 자동완성 후보를 최대 3개 반환합니다.")
    @GetMapping("/search/quick")
    ApiResponse<List<LinkuQuickSearchResponseDTO>> quickSearch(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String keyword
    );

    @Operation(summary = "링크 삭제", description = "사용자가 저장한 링크를 삭제합니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @DeleteMapping("/{userLinkuId}")
    ApiResponse<Object> deleteUsersLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userLinkuId
    );
}
