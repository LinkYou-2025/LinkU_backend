package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.common.KeywordNameResolver;
import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordServiceImpl implements KeywordService {
    private final UserRepository userRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final KeywordNameResolver keywordNameResolver;

    // 내 월별 상위 키워드 3개 조회
    @Override
    @Transactional(readOnly = true)
    public List<KeywordRankResponse> getMyTop3Keywords(Long userId, String month) {
        return keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonth(userId, month, PageRequest.of(0, 3))
                .stream()
                .map(k -> KeywordRankResponse.builder()
                        .name(keywordNameResolver.resolve(k.getType(), k.getRefId()))
                        .count(k.getCount())
                        .build())
                .toList();
    }

    // 같은 직업 유저들의 상위 키워드 15개 조회
    @Override
    @Transactional(readOnly = true)
    public List<KeywordRankResponse> getJobTop15Keywords(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (user.getJob() == null) {
            throw new GeneralException(UserErrorStatus._JOB_NOT_SET);
        }

        return keywordMonthlyCountRepository
                .findTopKeywordByJobId(user.getJob().getId(), PageRequest.of(0, 15))
                .stream()
                .map(row -> KeywordRankResponse.builder()
                        .name(keywordNameResolver.resolve((KeywordType) row[0], (Long) row[1]))
                        .count((Long) row[2])
                        .build())
                .toList();
    }
}
