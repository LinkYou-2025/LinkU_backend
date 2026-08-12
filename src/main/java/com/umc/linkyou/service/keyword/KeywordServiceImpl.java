package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import com.umc.linkyou.web.dto.keyword.KeywordLinkuItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordServiceImpl implements KeywordService {

    private final LinkuKeywordRepository linkuKeywordRepository;
    private final KeywordUpsertService keywordUpsertService;
    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final LinkuRepository linkuRepository;

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

    @Override
    public List<JobKeywordRankResponse> getJobTopKeywords(Long userId, YearMonth month, int limit) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        if (user.getJob() == null) {
            throw new GeneralException(UserErrorStatus._JOB_NOT_SET);
        }

        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        return linkuKeywordRepository
                .findTopKeywordNamesByJobIdAndPeriod(user.getJob().getId(), start, end, PageRequest.of(0, limit))
                .stream()
                .map(row -> JobKeywordRankResponse.builder()
                        .name(row.name())
                        .count(row.count())
                        .build())
                .toList();
    }

    @Override
    public List<KeywordLinkuItemDTO> getLinkusByKeyword(Long userId, String keyword) {
        keywordRepository.findByName(keyword)
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._KEYWORD_NOT_FOUND));

        return linkuRepository.findUserLinksByExactKeyword(userId, keyword);
    }
}
