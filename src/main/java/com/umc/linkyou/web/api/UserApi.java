package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.user.UserRequestDTO;
import com.umc.linkyou.web.dto.user.UserResponseDTO;
import com.umc.linkyou.web.dto.user.MarketingAgreeResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 마이페이지 및 설정 관련 API")
@RequestMapping("/users")
public interface UserApi {
    // 회원정보 조회
    @Operation(
            summary = "마이페이지 조회",
            description = """
                    로그인한 사용자의 프로필 정보 및 활동 요약을 조회합니다.
                    - 사용자의 이메일, 닉네임, 직업, 사용 목적, 관심사 목록을 반환합니다.
                    - 현재 로그인한 소셜 제공자(GENERAL, KAKAO 등) 정보를 포함합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(authErrorStatus = {AuthErrorStatus.UNAUTHORIZED})
    @ApiErrorCode(errorStatus = {ErrorStatus._INTERNAL_SERVER_ERROR})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/me")
    ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(
            @CurrentUser CustomUserDetails userDetails);

    // 회원정보 수정
    @Operation(
            summary = "마이페이지 수정",
            description = """
                    사용자의 프로필 정보를 부분 변경합니다. (Partial Update)
                    - 요청 본문에 포함하지 않은 필드는 변경되지 않고 기존 값을 유지합니다.
                    - nickname: 변경 시 중복 여부를 체크합니다.
                    - jobId: 존재하는 Job ID여야 하며(1부터 시작), 포함 시에만 직업이 변경됩니다.
                    - purposes: 링크를 저장/활용하는 목적 리스트 (예: CAREER-취업/커리어 준비, STUDY-학업/리포트 정리, WORK-업무자료 아카이빙, SIDE_PROJECT-사이드 프로젝트/창업 준비, SELF_DEVELOPMENT-자기계발/정보 수집, LATER_READING-나중에 읽고 싶은 글, INSIGHTS-인사이트 모으기, CREATION_REFERENCE-블로그/콘텐츠 작성 참고용, OTHERS-기타). 포함 시 해당 리스트로 전체 대체됩니다. (빈 배열을 보내면 전부 삭제됨)
                    - interests: 관심 있는 콘텐츠 분야 리스트 (예: BUSINESS-비즈니스/마케팅, IT-IT/개발, DESIGN-디자인/크리에이티브, PSYCHOLOGY-심리/자기계발, CAREER-커리어/채용, CURRENT_EVENTS-시사/트렌드, STUDY-학업/리포트 참고, STARTUP-스타트업/창업, SOCIETY-사회/문화/환경, WRITING-글쓰기/콘텐츠 작성, INSIGHTS-책/인사이트 요약, COLLECT-모아두고 싶은 글). 포함 시 해당 리스트로 전체 대체됩니다. (빈 배열을 보내면 전부 삭제됨)
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {
            UserErrorStatus._USER_NOT_FOUND,
            UserErrorStatus._DUPLICATE_NICKNAME,
            UserErrorStatus._INVALID_JOB_ID,
            UserErrorStatus._INVALID_PURPOSE,
            UserErrorStatus._INVALID_INTEREST
    })
    @PatchMapping("/profile")
    ApiResponse<Object> updateUserProfile(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.UpdateProfileDTO updateDTO);

    @Operation(
            summary = "회원 탈퇴 (비활성화)",
            description = """
                    사용자 계정을 비활성화(INACTIVE) 처리합니다.
                    - 탈퇴 사유를 입력받아 저장합니다.
                    - 실제 데이터 삭제는 정책에 따라 유예 기간(예: 30일) 후에 진행됩니다.
                    - 리프레시 토큰을 즉시 삭제하고, 현재 액세스 토큰을 블랙리스트에 등록하여 탈퇴 즉시 로그아웃 처리됩니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/inactive")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.DeleteReasonDTO deleteReasonDTO,
            HttpServletRequest request);

    // 소셜 프로필 완성 temp -> active
    @Operation(
            summary = "소셜 프로필 완성",
            description = """
                    소셜 로그인 직후 TEMP 상태인 사용자의 필수 추가 정보를 입력받습니다.
                    - 닉네임, 성별, 직업, 목적, 관심사, deviceId, deviceType을 입력받아 계정을 ACTIVE 상태로 전환합니다.
                    - 완료 시 정식 액세스/리프레시 토큰을 발급합니다.
                    - 최초 1회만 호출 가능하며, 이미 ACTIVE인 경우 에러를 반환합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._DUPLICATE_JOIN_REQUEST, UserErrorStatus._JOB_NOT_SET, UserErrorStatus._DUPLICATE_NICKNAME, UserErrorStatus._USER_NOT_FOUND})
    @PatchMapping("/social/complete")
    ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(
            @RequestBody @Valid UserRequestDTO.SocialCompleteDTO request,
            @CurrentUser CustomUserDetails userDetails);

    @Operation(
            summary = "약관 일괄 변경",
            description = "전달받은 약관 맵(termsMap)을 통해 동의 상태를 일괄 수정합니다. 기존 기록이 없으면 생성, 있으면 업데이트합니다." // 코드리뷰: 설명 문구 수정
    )
    // 첫 번째 PR: Common200 대신 SuccessStatus 또는 도메인별 성공코드 사용 규격 적용
    @ApiSuccessCode(com.umc.linkyou.apiPayload.code.status.SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus.INVALID_TERMS_TYPE})
    @PatchMapping("/terms/agree") // 두 번째 PR: POST에서 PATCH로 변경 및 경로 일치
    ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.TermsAgreeDTO request); // 두 번째 PR: Map 기반 DTO 사용

    // 약관 상태 조회
    @Operation(
            summary = "약관 상태 조회",
            description = "사용자가 현재 어떤 약관에 동의했는지 전체 목록과 상태를 조회합니다."
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/terms/status")
    ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(
            @CurrentUser CustomUserDetails userDetails);


    // 회원 탈퇴 복구 - 14일 이내
    @Operation(
            summary = "회원탈퇴 복구 (계정 활성화)",
            description = """
                탈퇴 유예 기간(14일) 내에 있는 사용자의 계정을 다시 활성화합니다.
                - 상태를 ACTIVE로 변경하고 탈퇴 사유 및 날짜를 초기화합니다.
                - 유예 기간(14일)이 지난 경우 복구 불가 에러를 반환합니다.
                - 복구 성공 시 별도 재로그인 없이 바로 홈 화면에 진입할 수 있도록 정식 액세스/리프레시 토큰 쌍을 함께 발급합니다.

                Swagger 테스트 시 유의사항
                - 회원탈퇴(inactive) API를 호출하면 기존 액세스/리프레시 토큰이 즉시 무효화(블랙리스트 등록/삭제)됩니다.
                - 따라서 탈퇴 직후 이 API를 테스트하려면, 탈퇴로 만료된 토큰이 아니라 로그인 API를 다시 호출해서 새로 발급받은 토큰을 Authorize에 넣어야 합니다.
                  (탈퇴 유예 기간 내 로그인 시 일반 액세스 토큰이 아닌, 이 API 전용의 복구 토큰(만료 10분)이 발급됩니다.)
                - 소셜 로그인 계정은 이메일/비밀번호가 없어 Swagger에서 곧바로 재로그인할 수 없으므로, 모바일 소셜 로그인 API를 통해 토큰을 재발급받아야 합니다.
                """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/recover")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> recoverMe(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.RecoverDTO request);

    // 계정 즉시 완전 삭제 (QA/테스트용, 대상은 항상 로그인한 본인 계정)
    @Operation(
            summary = "계정 즉시 완전 삭제 (테스트용)",
            description = """
                    로그인(authorization)한 사용자 본인 계정을 유예 기간 없이 즉시 완전 삭제합니다.
                    - 별도의 상태 조건 없이 요청 즉시 삭제되며, 연관 데이터는 DB의 ON DELETE CASCADE로 함께 삭제됩니다.
                    - 대상은 항상 요청자 본인 계정으로 한정되는 QA/테스트 목적의 엔드포인트입니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/test/delete-inactive")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(
            @CurrentUser CustomUserDetails userDetails);

    // 마케팅 약관 동의 또는 비동의
    @Operation(
            summary = "마케팅 약관 동의 토글",
            description = "사용자의 마케팅 약관 동의 상태를 토글합니다. 현재 동의 상태가 true이면 false로, false이면 true로 변경됩니다."
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PatchMapping("/terms/marketing/toggle")
    ApiResponse<MarketingAgreeResponseDTO> toggleMarketing(
            @CurrentUser CustomUserDetails userDetails);
}
