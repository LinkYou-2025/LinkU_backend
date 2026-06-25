package com.umc.linkyou.service.Linku;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class LinkuViewService {

    private final UsersLinkuRepository usersLinkuRepository;
    private final LinkuRepository linkuRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(Long userLinkuId, Long linkuId) {
        usersLinkuRepository.incrementViewCount(userLinkuId, LocalDateTime.now());
        linkuRepository.incrementTotalViewCount(linkuId);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRecommendKeywordCount(Long userId, Long emotionId, Long situationId) {
        String baseMonth = YearMonth.now().toString();
        keywordMonthlyCountRepository.upsertCount(userId, KeywordType.EMOTION.name(), emotionId, baseMonth);
        keywordMonthlyCountRepository.upsertCount(userId, KeywordType.SITUATION.name(), situationId, baseMonth);
    }
}
