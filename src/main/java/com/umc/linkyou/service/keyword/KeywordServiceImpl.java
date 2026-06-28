package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.common.KeywordNameResolver;
import com.umc.linkyou.service.keyword.KeywordUpsertService;
import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordServiceImpl implements KeywordService {

    private final UserRepository userRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final KeywordNameResolver keywordNameResolver;
    private final KeywordRepository keywordRepository;
    private final LinkuKeywordRepository linkuKeywordRepository;
    private final KeywordUpsertService keywordUpsertService;

    @Override
    @Transactional(readOnly = true)
    public List<KeywordRankResponse> getMyTopKeywords(Long userId, String month, int limit) {
        return keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonth(userId, month, PageRequest.of(0, limit))
                .stream()
                .map(k -> KeywordRankResponse.builder()
                        .name(keywordNameResolver.resolve(k.getType(), k.getRefId()))
                        .count(k.getCount())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeywordRankResponse> getJobTopKeywords(Long userId, int limit) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (user.getJob() == null) {
            throw new GeneralException(UserErrorStatus._JOB_NOT_SET);
        }

        return keywordMonthlyCountRepository
                .findTopKeywordByJobId(user.getJob().getId(), PageRequest.of(0, limit))
                .stream()
                .map(row -> KeywordRankResponse.builder()
                        .name(keywordNameResolver.resolve(row.type(), row.refId()))
                        .count(row.totalCount())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void saveKeywords(Linku linku, String rawKeywords) {
        if (rawKeywords == null || rawKeywords.isBlank()) return;

        for (String part : rawKeywords.split(",")) {
            String name = part.strip().replaceAll("^#", "");
            if (name.isBlank()) continue;

            Keyword keyword = keywordUpsertService.upsert(name);

            if (!linkuKeywordRepository.existsByLinkuAndKeyword(linku, keyword)) {
                linkuKeywordRepository.save(
                        LinkuKeyword.builder().linku(linku).keyword(keyword).build()
                );
            }
        }
    }
}
