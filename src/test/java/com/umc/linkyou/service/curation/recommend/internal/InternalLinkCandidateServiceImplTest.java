package com.umc.linkyou.service.curation.recommend.internal;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.mapping.SituationCategoryRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalLinkCandidateServiceImplTest {
    @InjectMocks
    private InternalLinkCandidateServiceImpl service;

    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private CurationRepository curationRepository;
    @Mock private KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    @Mock private SituationCategoryRepository situationCategoryRepository;

    private static final Long USER_ID = 48L;
    private static final Long CURATION_ID = 181L;
    private static final String MONTH = "2026-04";

    private static final long EMOTION_JOY = 1L;
    private static final long EMOTION_EXCITEMENT = 3L;
    private static final long EMOTION_SADNESS = 4L;

    private static final long SITUATION_ID = 5L;
    private static final long AI_CATEGORY_ID = 10L;

    private static final long LINK_ID_1 = 1L;
    private static final long LINK_ID_2 = 2L;
    private static final long LINK_ID_3 = 3L;

    private Curation makeCuration() {
        return Curation.builder()
                .month(MONTH)
                .build();
    }

    private UsersLinku makeLink(Long userLinkuId, Long emotionId, Long aiCategoryId, LocalDateTime createdAt) {
        Domain domain = Domain.builder().name("example.com").imageUrl("https://img.example.com").build();
        AiArticle aiArticle = aiCategoryId != null
                ? AiArticle.builder().aiCategoryId(aiCategoryId).build()
                : null;
        Linku linku = Linku.builder()
                .linku("https://example.com/" + userLinkuId)
                .title("Title " + userLinkuId)
                .domain(domain)
                .aiArticle(aiArticle)
                .build();
        Emotion emotion = Emotion.builder().emotionId(emotionId).build();
        UsersLinku ul = UsersLinku.builder()
                .userLinkuId(userLinkuId)
                .emotion(emotion)
                .linku(linku)
                .build();
        ul.setCreatedAt(createdAt);
        return ul;
    }

    private KeywordMonthlyCount makeKmc(KeywordType type, Long refId) {
        return KeywordMonthlyCount.builder()
                .type(type)
                .refId(refId)
                .baseMonth(MONTH)
                .count(5)
                .build();
    }

    @Test
    @DisplayName("해당 월에 저장한 링크가 없으면 빈 리스트를 반환한다")
    void noLinks_returnsEmpty() {
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(makeCuration()));
        when(usersLinkuRepository.findAllByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of());

        List<UsersLinku> result = service.getInternalCandidates(USER_ID, CURATION_ID, 4);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("감정+상황 점수가 높은 링크가 먼저 반환된다")
    void scoring_highScoreFirst() {
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(makeCuration()));

        LocalDateTime base = LocalDateTime.of(2026, 3, 15, 12, 0);
        // link1: 감정 정확일치(60) + 상황 일치(40) = 100
        // link2: 감정 유사(40) + 상황 불일치(0) = 40
        // link3: 감정 불일치(0) + 상황 불일치(0) = 0
        UsersLinku link1 = makeLink(LINK_ID_1, EMOTION_JOY, AI_CATEGORY_ID, base);
        UsersLinku link2 = makeLink(LINK_ID_2, EMOTION_EXCITEMENT, null, base);
        UsersLinku link3 = makeLink(LINK_ID_3, EMOTION_SADNESS, null, base);

        when(usersLinkuRepository.findAllByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of(link3, link2, link1));

        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.EMOTION), any(PageRequest.class)))
                .thenReturn(List.of(makeKmc(KeywordType.EMOTION, EMOTION_JOY)));

        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.SITUATION), any(PageRequest.class)))
                .thenReturn(List.of(makeKmc(KeywordType.SITUATION, SITUATION_ID)));

        when(situationCategoryRepository.findCategoryIdsBySituationId(SITUATION_ID))
                .thenReturn(List.of(AI_CATEGORY_ID));

        List<UsersLinku> result = service.getInternalCandidates(USER_ID, CURATION_ID, 4);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getUserLinkuId()).isEqualTo(LINK_ID_1); // 100점
        assertThat(result.get(1).getUserLinkuId()).isEqualTo(LINK_ID_2); // 40점
        assertThat(result.get(2).getUserLinkuId()).isEqualTo(LINK_ID_3); // 0점
    }

    @Test
    @DisplayName("limit 개수만큼만 반환된다")
    void limit_applied() {
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(makeCuration()));

        LocalDateTime base = LocalDateTime.now();
        List<UsersLinku> links = List.of(
                makeLink(LINK_ID_1, EMOTION_JOY, null, base),
                makeLink(LINK_ID_2, EMOTION_JOY, null, base),
                makeLink(LINK_ID_3, EMOTION_JOY, null, base)
        );
        when(usersLinkuRepository.findAllByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(links);
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.EMOTION), any(PageRequest.class)))
                .thenReturn(List.of());
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.SITUATION), any(PageRequest.class)))
                .thenReturn(List.of());

        List<UsersLinku> result = service.getInternalCandidates(USER_ID, CURATION_ID, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("점수가 동일하면 최신 링크가 먼저 반환된다")
    void tieBreak_byCreatedAtDesc() {
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(makeCuration()));

        LocalDateTime older = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 3, 20, 0, 0);
        UsersLinku oldLink = makeLink(LINK_ID_1, EMOTION_SADNESS, null, older); // 동점 (0점)
        UsersLinku newLink = makeLink(LINK_ID_2, EMOTION_SADNESS, null, newer); // 동점 (0점)

        when(usersLinkuRepository.findAllByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of(oldLink, newLink));
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.EMOTION), any(PageRequest.class)))
                .thenReturn(List.of());
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.SITUATION), any(PageRequest.class)))
                .thenReturn(List.of());

        List<UsersLinku> result = service.getInternalCandidates(USER_ID, CURATION_ID, 4);

        assertThat(result.get(0).getUserLinkuId()).isEqualTo(LINK_ID_2); // 최신이 먼저
        assertThat(result.get(1).getUserLinkuId()).isEqualTo(LINK_ID_1);
    }

    @Test
    @DisplayName("topEmotion이 없으면 감정 점수는 모두 0이다")
    void noTopEmotion_zeroEmotionScore() {
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(makeCuration()));

        LocalDateTime base = LocalDateTime.now();
        UsersLinku link1 = makeLink(LINK_ID_1, EMOTION_JOY, null, base.minusDays(1));
        UsersLinku link2 = makeLink(LINK_ID_2, EMOTION_EXCITEMENT, null, base);

        when(usersLinkuRepository.findAllByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of(link1, link2));
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.EMOTION), any(PageRequest.class)))
                .thenReturn(List.of());
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonthAndType(
                eq(USER_ID), eq(MONTH), eq(KeywordType.SITUATION), any(PageRequest.class)))
                .thenReturn(List.of());

        List<UsersLinku> result = service.getInternalCandidates(USER_ID, CURATION_ID, 4);

        // 감정 점수 0, 상황 점수 0 → 동점 → 최신 link2가 먼저
        assertThat(result.get(0).getUserLinkuId()).isEqualTo(LINK_ID_2);
        assertThat(result.get(1).getUserLinkuId()).isEqualTo(LINK_ID_1);
    }
}
