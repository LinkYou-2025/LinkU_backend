package com.umc.linkyou.batch.user;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.redis.FcmTokenRedisRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 비활성 사용자 삭제 결과를 반영하는 writer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveUserItemWriter implements ItemWriter<Users> {

    private final UserRepository userRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final FcmTokenRedisRepository fcmTokenRedisRepository;

    @Override
    public void write(Chunk<? extends Users> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        List<Long> userIds = chunk.getItems().stream()
                .map(Users::getId)
                .toList();

        termsAgreementRepository.deleteAllByUserIdIn(userIds);
        userRepository.deleteAll(chunk.getItems());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long userId : userIds) {
                    try {
                        refreshTokenManager.deleteAllTokens(userId);
                        fcmTokenRedisRepository.deleteById(userId);
                    } catch (Exception e) {
                        log.warn("[BATCH] Redis token cleanup failed for userId={}", userId, e);
                    }
                }
            }
        });
    }
}
