package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawService{

    private final UserRepository userRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthAccountRepository authAccountRepository;

    @Transactional
    public Users withdrawUser(Long userId, UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        user.setStatus(UserStatus.INACTIVE);
        user.setInactiveDate(LocalDateTime.now());
        user.setDeleted_reason(deleteReasonDTO.getReason());
        userRepository.save(user);
        return user;
    }
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteCompletelyInactiveUsers() {
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        List<Long> inactiveUserIds = userRepository.findInactiveUserIds(tenDaysAgo);

        if (inactiveUserIds.isEmpty()) {
            log.debug("삭제할 비활성 사용자 없음");
            return;
        }

        // 1. Redis 삭제
        for (Long userId : inactiveUserIds) {
            refreshTokenManager.deleteAllTokens(userId);
        }

        // 2. 엔티티 로드
        List<Users> toDelete = userRepository.findAllById(inactiveUserIds);

        // 3. 연관관계 수동 정리 (FK 제약 조건 방지)
        for (Users user : toDelete) {
            // 자식의 자식 (LinkuFolder)부터 순차 삭제 트리거
            user.getUsersLinkus().forEach(ul -> ul.getLinkuFolders().clear());

            // Users 엔티티에 연결된 모든 리스트 clear
            // orphanRemoval = true 설정에 의해 DB 삭제 쿼리가 예약됩니다.
            user.getUsersLinkus().clear();
            user.getUserAlarms().clear();
            user.getUserFcmTokens().clear();
            user.getCurations().clear();
            user.getCurationLikes().clear();
            user.getEmotionLogs().clear();
            user.getFolderShareLinks().clear();
            user.getRecentViewedLinkus().clear();
            user.getUsersFoldersList().clear();
            user.getUsersCategoryColorList().clear(); // 서버 에러 포인트 해결
            user.getPurposes().clear();
            user.getInterests().clear();
            user.getAuthAccounts().clear();
        }

        // 4. 부모 엔티티 삭제
        // deleteAllInBatch 대신 deleteAll을 사용하여 영속성 컨텍스트를 거쳐 안전하게 삭제합니다.
        userRepository.deleteAll(toDelete);

        log.info("🗑️ 비활성 사용자 {}명 및 모든 연관 데이터 완전삭제 완료", toDelete.size());
    }

    // 🔥 테스트 메서드 (단일 삭제용)
    @Transactional
    public void testImmediateDelete(Long userId) {
        if (userId == null) {
            log.info("testImmediateDelete: userId 없음");
            return;
        }

        // 1. Redis 삭제
        refreshTokenManager.deleteAllTokens(userId);
        log.debug("Redis 테스트삭제: userId={}", userId);

        // 2. 엔티티 로드 및 정리
        userRepository.findById(userId).ifPresent(user -> {
            // 깊은 관계부터 정리
            user.getUsersLinkus().forEach(ul -> ul.getLinkuFolders().clear());
            user.getUsersLinkus().clear();

            // 모든 컬렉션 clear
            user.getUserAlarms().clear();
            user.getUserFcmTokens().clear();
            user.getCurations().clear();
            user.getCurationLikes().clear();
            user.getEmotionLogs().clear();
            user.getFolderShareLinks().clear();
            user.getRecentViewedLinkus().clear();
            user.getUsersFoldersList().clear();
            user.getUsersCategoryColorList().clear();
            user.getPurposes().clear();
            user.getInterests().clear();
            user.getAuthAccounts().clear();

            // 3. 최종 삭제
            userRepository.delete(user);
            log.warn("🧪 테스트삭제 완료: userId={}", userId);
        });
    }

    @Transactional
    public void handleKakaoUnlinkWebhook(String kakaoExternalId) {
        // 1. 카카오 연동 정보 조회 (기존 Custom 인터페이스의 findByProviderAndExternalId 활용)
        AuthAccount kakaoAccount = authAccountRepository.findByProviderAndExternalId(Provider.KAKAO, kakaoExternalId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

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
