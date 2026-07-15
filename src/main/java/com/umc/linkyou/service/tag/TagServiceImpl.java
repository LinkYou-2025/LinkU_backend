package com.umc.linkyou.service.tag;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.common.TagNameResolver;
import com.umc.linkyou.web.dto.tag.MyTagRankResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final UserRepository userRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final TagNameResolver tagNameResolver;

    @Override
    @Transactional(readOnly = true)
    public List<MyTagRankResponse> getMyTopTags(Long userId, YearMonth month, int limit) {
        if (!userRepository.existsById(userId)) {
            throw new GeneralException(UserErrorStatus._USER_NOT_FOUND);
        }

        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        List<TagStatRow> emotionCounts = usersLinkuRepository
                .countByEmotionForUserAndPeriod(userId, start, end).stream()
                .map(row -> new TagStatRow(KeywordType.EMOTION, (Long) row[0], (Long) row[1]))
                .toList();
        List<TagStatRow> situationCounts = usersLinkuRepository
                .countBySituationForUserAndPeriod(userId, start, end).stream()
                .map(row -> new TagStatRow(KeywordType.SITUATION, (Long) row[0], (Long) row[1]))
                .toList();

        long total = Stream.concat(emotionCounts.stream(), situationCounts.stream())
                .mapToLong(TagStatRow::totalCount)
                .sum();

        return Stream.concat(emotionCounts.stream(), situationCounts.stream())
                .sorted(Comparator.comparingLong(TagStatRow::totalCount).reversed())
                .limit(limit)
                .map(row -> MyTagRankResponse.builder()
                        .name(tagNameResolver.resolve(row.type(), row.refId()))
                        .percent(total == 0 ? 0 : (int) Math.round(row.totalCount() * 100.0 / total))
                        .build())
                .toList();
    }
}
