package com.umc.linkyou.service.Linku;

import static com.umc.linkyou.support.fixture.HomeRecommendFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.RecommendCursorUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuRecommendService 단위 테스트")
class LinkuRecommendServiceTest {
    @InjectMocks private LinkuRecommendService service;
    @Mock private EmotionRepository emotionRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private UserRepository userRepository;
    @Mock private SituationRepository situationRepository;
    @Mock private SituationJobRepository situationJobRepository;
    @Mock private LinkuViewService linkuViewService;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private SituationCategoryService situationCategoryService;

    private void context(long count) {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(emotionRepository.findById(EMOTION_ID)).willReturn(Optional.of(emotion()));
        given(situationRepository.findById(SITUATION_ID)).willReturn(Optional.of(situation()));
        given(situationJobRepository.findBySituation_IdAndJob_Id(SITUATION_ID, JOB_ID))
                .willReturn(Optional.of(situationJob()));
        given(usersLinkuRepository.countByUser_Id(USER_ID)).willReturn(count);
    }

    private void candidates(List<RankedUsersLinku> rows, Integer score, Long id, int limit) {
        given(situationCategoryService.getCategoryIdsBySituation(SITUATION_ID))
                .willReturn(List.of(CATEGORY_ID));
        given(usersLinkuRepository.findNormalRecommendCandidates(eq(USER_ID), eq(EMOTION_ID),
                eq(SITUATION_ID), eq(List.of(CATEGORY_ID)), any(LocalDateTime.class),
                eq(score), eq(id), eq(limit))).willReturn(rows);
    }

    @Nested
    @DisplayName("성공")
    class Success {
        @Test
        @DisplayName("다음_후보가_있을_시_노출한_마지막_항목의_커서를_반환한다")
        void 다음_후보가_있을_시_노출한_마지막_항목의_커서를_반환한다() {
            context(3);
            candidates(List.of(candidate(30L, 150), candidate(20L, 120), candidate(10L, 100)), null, null, 3);
            given(linkuFolderRepository.findByUsersLinku_UserLinkuIdIn(List.of(30L, 20L)))
                    .willReturn(List.of(folder(30L, 3L, "최신 폴더"), folder(30L, 2L, "이전 폴더")));
            var response = service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, 2);
            assertTrue(response.getIsSuccess());
            assertEquals("COMMON200", response.getCode());
            var page = response.getResult();
            assertThat(page.items()).extracting(item -> item.userLinkuId()).containsExactly(30L, 20L);
            assertEquals("최신 폴더", page.items().get(0).folderName());
            assertNull(page.items().get(1).folderName());
            assertTrue(page.hasNext());
            assertEquals(new RecommendCursorUtil.RecommendCursor(120, 20L),
                    RecommendCursorUtil.decode(page.nextCursor()));
            verify(linkuViewService).recordRecommendKeywordCount(USER_ID, EMOTION_ID, SITUATION_ID);
        }

        @Test
        @DisplayName("마지막_페이지_조회_시_다음_커서없이_결과를_반환한다")
        void 마지막_페이지_조회_시_다음_커서없이_결과를_반환한다() {
            context(3);
            candidates(List.of(candidate(10L, 100)), 120, 20L, 2);
            String cursor = RecommendCursorUtil.encode(new RecommendCursorUtil.RecommendCursor(120, 20L));
            var page = service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, cursor, 1).getResult();
            assertEquals(10L, page.items().get(0).userLinkuId());
            assertFalse(page.hasNext());
            assertNull(page.nextCursor());
        }

        @Test
        @DisplayName("추천_결과가_없을_시_폴더조회와_통계저장없이_빈_결과를_반환한다")
        void 추천_결과가_없을_시_폴더조회와_통계저장없이_빈_결과를_반환한다() {
            context(3);
            candidates(List.of(), null, null, 6);
            var page = service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, 5).getResult();
            assertEquals(List.of(), page.items());
            assertFalse(page.hasNext());
            assertNull(page.nextCursor());
            verifyNoInteractions(linkuFolderRepository, linkuViewService);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("조회_개수가_0이하일_시_후보조회없이_빈_결과를_반환한다")
        void 조회_개수가_0이하일_시_후보조회없이_빈_결과를_반환한다(int size) {
            context(3);
            var page = service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, size).getResult();
            assertEquals(List.of(), page.items());
            assertFalse(page.hasNext());
            assertNull(page.nextCursor());
            verifyNoInteractions(situationCategoryService, linkuFolderRepository, linkuViewService);
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {
        @Test
        @DisplayName("직업이_없을_시_JOB_NOT_SET_예외를_반환한다")
        void 직업이_없을_시_JOB_NOT_SET_예외를_반환한다() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(userWithoutJob()));
            given(emotionRepository.findById(EMOTION_ID)).willReturn(Optional.of(emotion()));
            given(situationRepository.findById(SITUATION_ID)).willReturn(Optional.of(situation()));
            var ex = assertThrows(GeneralException.class,
                    () -> service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, 5));
            assertEquals(UserErrorStatus._JOB_NOT_SET, ex.getCode());
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, 2L})
        @DisplayName("저장_링크가_3개_미만일_시_개수에_맞는_에러를_반환한다")
        void 저장_링크가_3개_미만일_시_개수에_맞는_에러를_반환한다(long count) {
            context(count);
            var ex = assertThrows(GeneralException.class,
                    () -> service.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, 5));
            assertEquals(count == 0 ? ErrorStatus._RECOMMEND_LINKU_NEW_USER
                    : ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS, ex.getCode());
            verifyNoInteractions(situationCategoryService, linkuViewService, linkuFolderRepository);
        }
    }
}
