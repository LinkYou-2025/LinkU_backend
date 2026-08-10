package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawService{

    private final UserRepository userRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthAccountRepository authAccountRepository;
    private final UserStatusValidator userStatusValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlackListManager accessTokenBlackListManager;
    private final TokenIssueService tokenIssueService;

    // 탈퇴 유예 기간
    private static final int GRACE_PERIOD_DAYS = 14;

    /**
     * 회원 탈퇴 (accessToken 미지정)
     * 웹훅 등 현재 요청의 accessToken을 알 수 없는 내부 호출용.
     */
    @Transactional
    public Users withdrawUser(Long userId, UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        return withdrawUser(userId, deleteReasonDTO, null);
    }

    /**
     * 회원 탈퇴
     * - Refresh Token 전체 삭제
     * - 현재 요청의 Access Token을 블랙리스트에 등록하여 탈퇴 즉시 로그아웃 처리
     */
    @Transactional
    public Users withdrawUser(
            Long userId, UserRequestDTO.DeleteReasonDTO deleteReasonDTO, String accessToken) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        user.withdraw(deleteReasonDTO.getReason(), LocalDateTime.now());
        userRepository.save(user);

        // DB 커밋 후에만 Redis 반영 (커밋 실패 시 Redis는 건드리지 않음)
        runAfterCommit(() -> invalidateTokens(userId, accessToken));

        return user;
    }

    /**
     * 트랜잭션 동기화가 활성 상태면 커밋 후에 실행되도록 등록하고,
     * 활성 상태가 아니면(예: 순수 단위 테스트처럼 트랜잭션 밖에서 호출된 경우) 즉시 실행한다.
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 리프레시 토큰 전체 삭제 + 현재 액세스 토큰 블랙리스트 등록.
     */
    private void invalidateTokens(Long userId, String accessToken) {
        try {
            refreshTokenManager.deleteAllTokens(userId);
        } catch (Exception e) {
            log.warn("탈퇴 처리 중 리프레시 토큰 삭제 실패 (userId={})", userId, e);
        }
        blacklistAccessToken(accessToken);
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        // 만료/위조 등으로 파싱이 실패해도 탈퇴 자체는 계속 진행되어야 하므로
        // 블랙리스트 등록 실패는 로그만 남기고 흐름을 막지 않는다.
        try {
            long ttlMs = jwtTokenProvider.getRemainingExpiryMs(accessToken);
            if (ttlMs > 0) {
                accessTokenBlackListManager.addToBlacklist(accessToken, ttlMs);
            }
        } catch (Exception e) {
            log.warn("탈퇴 처리 중 액세스 토큰 블랙리스트 등록 실패 (탈퇴는 계속 진행됨)", e);
        }
    }

    /**
     * 회원 탈퇴 복구 API
     * - INACTIVE 상태의 사용자를 다시 ACTIVE로 전환합니다.
     * - 복구 성공 시 재로그인 없이 바로 홈 화면에 진입할 수 있도록 정식 액세스/리프레시 토큰 쌍을 함께 발급
     */
    @Transactional
    public UserResponseDTO.withDrawalResultDTO recoverUser(
            Long userId, String providerStr, UserRequestDTO.RecoverDTO request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.INACTIVE) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST); // 이미 활성 상태인 경우
        }
        // 2. 유예 기간(14일) 이내인지 확인
        if (!userStatusValidator.isWithinWithdrawGracePeriod(user)) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        // 상태 및 탈퇴 관련 필드 초기화
        user.recover();
        Users savedUser = userRepository.save(user);

        // 3. 복구 완료 시점에 정식 토큰 쌍 발급
        Provider provider = Provider.valueOf(providerStr);
        String email =
                authAccountRepository
                        .findEmailByUserIdAndProvider(userId, provider)
                        .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        TokenIssueService.IssuedTokenPair tokenPair =
                tokenIssueService.issueTokenPair(
                        userId,
                        email,
                        providerStr,
                        savedUser.getRole(),
                        request.deviceId(),
                        request.deviceType());

        return UserConverter.toWithDrawalResultDTO(
                savedUser, tokenPair.accessToken(), tokenPair.refreshToken());
    }

    //14 일 이후 삭제
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteCompletelyInactiveUsers() {
        LocalDateTime daysAgo = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        List<Long> inactiveUserIds = userRepository.findInactiveUserIds(daysAgo);

        if (inactiveUserIds.isEmpty()) {
            log.debug("삭제할 비활성 사용자 없음");
            return;
        }

        // 1. DB 엔티티 삭제 트랜잭션
        List<Users> toDelete = userRepository.findAllById(inactiveUserIds);
        userRepository.deleteAll(toDelete);

        // 2. DB 커밋 후에만 Redis 토큰 삭제
        runAfterCommit(() -> {
            for (Long userId : inactiveUserIds) {
                try {
                    refreshTokenManager.deleteAllTokens(userId);
                } catch (Exception e) {
                    log.warn("완전삭제 후 리프레시 토큰 삭제 실패 (userId={})", userId, e);
                }
            }
        });

        log.info("🗑️ 비활성 사용자 {}명 및 모든 연관 데이터 완전삭제 완료", toDelete.size());
    }
    /**
     * 테스트용 단일 즉시 삭제 메소드
     * @param userId 삭제할 사용자 ID
     * @return 삭제된 사용자 엔티티 (컨버터에서 사용 가능)
     */
    @Transactional
    public Users testImmediateDelete(Long userId) {
        // 1. 엔티티 로드 (없으면 예외 발생)
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        // 2. 부모 엔티티 삭제. 연관 데이터는 DB ON DELETE CASCADE가 정리합니다.
        userRepository.delete(user);

        // 3. Redis 토큰 삭제는 DB 커밋 후에만 (삭제가 롤백되면 Redis는 건드리지 않음)
        runAfterCommit(() -> {
            try {
                refreshTokenManager.deleteAllTokens(userId);
            } catch (Exception e) {
                log.warn("테스트 삭제 후 리프레시 토큰 삭제 실패 (userId={})", userId, e);
            }
        });

        log.info("🧪 테스트 삭제 완료: 사용자 ID {}", userId);

        return user; // 삭제된 상태의 엔티티 객체 반환
    }

    @Transactional
    public void handleKakaoUnlinkWebhook(String kakaoExternalId) {
        // 1. 카카오 연동 정보 조회 (기존 Custom 인터페이스의 findByProviderAndExternalId 활용)
        AuthAccount kakaoAccount = authAccountRepository.findByProviderAndExternalId(Provider.KAKAO, kakaoExternalId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        Users user = kakaoAccount.getUser();

        // 2. 카카오 연동 정보(AuthAccount) 삭제
        // 여기서 delete를 호출하면 DB 제약조건에 따라 AuthAccount만 먼저 삭제됩니다.
        authAccountRepository.delete(kakaoAccount);

        // 영속성 컨텍스트에 반영하여 개수 체크 시 정확도 보장
        authAccountRepository.flush();

        // 3. 다른 로그인 수단이 남아있는지 확인 (새로 만든 exists 메서드 활용)
        boolean hasOtherAccounts = authAccountRepository.existsByUserIdAndProviderNot(user.getId(), Provider.KAKAO);

        if (!hasOtherAccounts) {
            // 4. 남은 계정이 없으면 유저 상태를 INACTIVE로 변경 (기존 withdrawUser 재사용)
            UserRequestDTO.DeleteReasonDTO reason = new UserRequestDTO.DeleteReasonDTO();
            reason.setReason("카카오 연결 해제 웹훅에 의한 자동 탈퇴");
            this.withdrawUser(user.getId(), reason);
            //카카오 외 계정 없음 -> 전체 탈퇴 처리됨"
        } else {
            //다른 계정 존재 -> 카카오 연동 정보만 해제됨
        }
    }
}
