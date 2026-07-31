package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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

    @Operation(summary = "저번 달 미열람 링크 조회", description = "저번 달에 저장만 하고 한 번도 열어보지 않은 링크 목록을 조회합니다.")
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/unread")
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getLastMonthUnreadLinkus(
            @CurrentUser CustomUserDetails userDetails
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

    @Operation(
            summary = "링크 추천",
            description = """
                    상황(situation)과 감정(emotion)을 기반으로 링크를 추천합니다. 커서 기반 페이지네이션을 지원합니다.

                    - **situationId**: 요청 유저의 직업(job)에 해당하는 상황 ID만 유효합니다. 직업과 맞지 않는 situationId를 넘기면 404(`_SITUATION_NOT_FOUND`)가 반환됩니다.
                      - job_id 1 (고등학생) → situation 1~8 (1통학 중, 2공부 중, 3식사 중, 4시험 준비, 5친구랑, 6쇼핑 중, 7휴식 중, 8자기 전)
                      - job_id 2 (대학생) → situation 9~16 (9과제 중, 10통학 중, 11쇼핑 중, 12알바 중, 13트렌드 확인, 14데이트 중, 15휴식 중, 16자기 전)
                      - job_id 3 (직장인) → situation 17~24 (17출퇴근, 18트렌드 확인, 19업무 중, 20커리어 고민, 21쇼핑 중, 22데이트 중, 23휴식 중, 24자기 전)
                      - job_id 4 (자영업자) → situation 25~32 (25출퇴근, 26업무 준비 중, 27데이트 중, 28식사, 29쇼핑 중, 30트렌드 확인, 31휴식 중, 32자기 전)
                      - job_id 5 (프리랜서) → situation 33~40 (33작업 중, 34쇼핑 중, 35트렌드 확인, 36데이트 중, 37운동 중, 38식사, 39휴식 중, 40자기 전)
                      - job_id 6 (취준생) → situation 41~48 (41자소서 작성, 42면접 준비, 43요리 중, 44트렌드 확인, 45쇼핑 중, 46운동 중, 47휴식 중, 48자기 전)
                    - **emotionId**: 1즐거움, 2평온, 3설렘, 4슬픔, 5짜증, 6분노 중 하나.
                    - 추천을 받으려면 저장한 링크가 3개 이상이어야 합니다(미만이면 `_RECOMMEND_LINKU_NOT_ENOUGH_LINKS`/`_RECOMMEND_LINKU_NEW_USER`).

                    **페이징 방식 (커서 기반)**
                    - "최근에 안 본 링크"(novelty)를 우선 노출하는 로직이 있어, 일반 후보군과 서로 다른 속도로
                      소진된다. 그래서 `page` 번호 대신 서버가 두 후보군의 진행 상태를 인코딩한 `cursor` 문자열을
                      내려주고, FE는 그 값을 그대로 다음 요청에 전달한다. `cursor` 안의 내용은 파싱/계산할 필요
                      없이 그대로 복사만 하면 된다.
                    - 첫 요청 시 `cursor` 파라미터를 생략한다.
                    - 응답의 `hasNext=true`이면, 응답의 `nextCursor` 값을 다음 요청의 `cursor`로 그대로 전달해
                      다음 페이지를 이어서 조회한다.
                    - `hasNext=false`이면 `nextCursor`는 null이며 더 이상 가져올 데이터가 없다는 뜻이다.
                    - 잘못된 `cursor` 값을 넘겨도 에러를 던지지 않고 첫 페이지로 안전하게 처리한다.
                    """
    )
    @ApiErrorCode(
            errorStatus = {ErrorStatus._SITUATION_NOT_FOUND, ErrorStatus._EMOTION_NOT_FOUND, ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS, ErrorStatus._RECOMMEND_LINKU_NO_RECOMMENDATION, ErrorStatus._RECOMMEND_LINKU_NEW_USER},
            userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus._JOB_NOT_SET}
    )
    @GetMapping("/recommend")
    ApiResponse<LinkuResponseDTO.LinkuRecommendCursorPageDTO> recommendLinku(
            @CurrentUser CustomUserDetails userDetails,
            @Parameter(description = "요청 유저의 job에 해당하는 상황 ID (설명 참고)", example = "19") @RequestParam Long situationId,
            @Parameter(description = "감정 ID: 1즐거움/2평온/3설렘/4슬픔/5짜증/6분노", example = "1") @RequestParam Long emotionId,
            @Parameter(description = "이전 응답의 nextCursor 값을 그대로 전달. 첫 요청 시 생략합니다.") @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int size
    );

    @Operation(summary = "링크 검색", description = "사용자가 저장한 링크에서 제목·태그가 검색어와 일치하는 링크 목록을 최신 저장 순으로 조회합니다. 커서는 필수입니다. 첫 페이지는 0, 이후에는 응답의 nextCursor 값을 보냅니다.")
    @GetMapping("/search")
    ApiResponse<LinkuSearchResponseDTO.LinkuSearchCursorPageResponse> searchLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam @Size(max = 20, message = "검색어는 20자 이하로 입력해주세요.") String searchQuery,
            @RequestParam(defaultValue = "0") @PositiveOrZero Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    );

    @Operation(summary = "검색어 자동완성", description = "사용자가 저장한 링크의 제목에서 검색어와 일치하는 자동완성 후보를 최대 3개 반환합니다.")
    @GetMapping("/search/quick")
    ApiResponse<List<LinkuQuickSearchResponseDTO>> quickSearch(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam @Size(max = 20, message = "검색어는 20자 이하로 입력해주세요.") String searchQuery
    );

    @Operation(summary = "링크 삭제", description = "사용자가 저장한 링크를 삭제합니다.")
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @DeleteMapping("/{userLinkuId}")
    ApiResponse<Object> deleteUsersLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userLinkuId
    );
}
