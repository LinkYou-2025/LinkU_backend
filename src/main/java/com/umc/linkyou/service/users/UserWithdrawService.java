package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawService{

    private final UserRepository userRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthAccountRepository authAccountRepository;

    // 탈퇴 유예 기간
    private static final int GRACE_PERIOD_DAYS = 14;

    @Transactional
    public Users withdrawUser(Long userId, UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        // 1. 토큰 즉시 무효화
        refreshTokenManager.deleteAllTokens(userId);
        user.withdraw(deleteReasonDTO.getReason(), LocalDateTime.now());
        userRepository.save(user);
        return user;
    }

    /**
     * 회원 탈퇴 복구 API
     * INACTIVE 상태의 사용자를 다시 ACTIVE로 전환합니다.
     */
    @Transactional
    public Users recoverUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.INACTIVE) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST); // 이미 활성 상태인 경우
        }
        // 2. inactiveDate가 null이거나 14일이 경과했는지 확인
        LocalDateTime inactiveDate = user.getInactiveDate();
        if (inactiveDate == null || ChronoUnit.DAYS.between(inactiveDate, LocalDateTime.now()) > GRACE_PERIOD_DAYS) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        // 상태 및 탈퇴 관련 필드 초기화
        user.recover();

        return userRepository.save(user);
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long userId : inactiveUserIds) {
                    refreshTokenManager.deleteAllTokens(userId);
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

        // 2. Redis 토큰 삭제
        refreshTokenManager.deleteAllTokens(userId);

        // 3. 부모 엔티티 삭제. 연관 데이터는 DB ON DELETE CASCADE가 정리합니다.
        userRepository.delete(user);

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
