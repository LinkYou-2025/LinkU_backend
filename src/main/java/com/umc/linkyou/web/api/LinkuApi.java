package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "링크 수정", description = "기존 링크의 정보(메모, 감정, 상황, 도메인, 제목 등)를 수정합니다. URL 자체는 이 API로 변경할 수 없으며, 폴더 이동도 이 API로 처리하지 않고 별도의 링크 폴더 이동 API를 사용해야 합니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._LINKU_NOT_FOUND, LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @PatchMapping(value = "/{linkuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO
    );

    @Operation(summary = "링크 폴더 이동", description = "링크가 속한 폴더를 변경합니다. 링크(Linku)는 동일 URL을 저장한 모든 유저가 공유하는 데이터이므로, 이 API는 해당 유저 소유의 폴더 매핑만 변경하며 링크 자체의 카테고리는 변경하지 않습니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND}, folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN})
    @PatchMapping(value = "/{linkuId}/folder", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuFolderChangeResultDTO> updateLinkuFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuFolderUpdateDTO updateDTO
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

    @Operation(summary = "빠른 검색 (사용자 저장 링크 전체 대상)", description = "사용자가 저장한 링크 전체를 대상으로 키워드가 포함된 추천 검색어 목록을 조회합니다.")
    @GetMapping("/search/quick")
    ApiResponse<List<LinkuSearchSuggestionResponse>> quickSearch(
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
