package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.LinkuSearchHistory;
import com.umc.linkyou.repository.LinkuSearchHistoryRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchHistoryItemDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.LinkuFixture.quickSearchItem;
import static com.umc.linkyou.support.fixture.LinkuFixture.searchHistory;
import static com.umc.linkyou.support.fixture.LinkuFixture.searchItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuSearchService 테스트")
class LinkuSearchServiceTest {

    @InjectMocks
    private LinkuSearchService linkuSearchService;

    @Mock
    private LinkuRepository linkuRepository;

    @Mock
    private LinkuSearchHistoryRepository linkuSearchHistoryRepository;

    @Nested
    @DisplayName("검색 (search)")
    class Search {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("퀵서치 - keyword 앞뒤 공백을 제거한다")
            void 퀵서치_앞뒤_공백을_제거한다() {
                when(linkuRepository.findQuickByKeyword(1L, "Java"))
                        .thenReturn(List.of(quickSearchItem("Java Guide", 1L)));

                List<LinkuQuickSearchResponseDTO> result = linkuSearchService.quickSearch(1L, "  Java  ");

                assertThat(result).hasSize(1);
                assertThat(result.get(0).title()).isEqualTo("Java Guide");
                verify(linkuRepository).findQuickByKeyword(1L, "Java");
            }

            @Test
            @DisplayName("검색 - size+1개가 조회되면 hasNext true, 마지막 항목은 잘라낸다")
            void 검색_결과가_size초과면_hasNext_true를_반환한다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 2))
                        .thenReturn(List.of(searchItem(30L, "A"), searchItem(20L, "B"), searchItem(10L, "C")));

                LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                        linkuSearchService.search(1L, "Java", null, 2);

                assertThat(result.hasNext()).isTrue();
                assertThat(result.items()).hasSize(2);
                assertThat(result.nextCursor()).isEqualTo(20L);
            }

            @Test
            @DisplayName("검색 - size 이하로 조회되면 hasNext false, nextCursor는 null")
            void 검색_결과가_size이하면_hasNext_false를_반환한다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of(searchItem(30L, "A")));

                LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                        linkuSearchService.search(1L, "Java", null, 10);

                assertThat(result.hasNext()).isFalse();
                assertThat(result.items()).hasSize(1);
                assertThat(result.nextCursor()).isNull();
            }

            @Test
            @DisplayName("검색 - 결과가 없으면 nextCursor는 null")
            void 검색_결과가_없으면_nextCursor는_null이다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of());

                LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                        linkuSearchService.search(1L, "Java", null, 10);

                assertThat(result.hasNext()).isFalse();
                assertThat(result.items()).isEmpty();
                assertThat(result.nextCursor()).isNull();
            }

            @Test
            @DisplayName("검색 - size가 0 이하면 빈 페이지를 반환한다")
            void 검색_size가_0이하면_빈_페이지를_반환한다() {
                LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                        linkuSearchService.search(1L, "Java", null, 0);

                assertThat(result.items()).isEmpty();
                assertThat(result.hasNext()).isFalse();
                verify(linkuRepository, never()).searchUserLinks(anyLong(), anyString(), any(), anyInt());
            }

            @Test
            @DisplayName("검색 성공 시 검색어 기록이 저장된다")
            void 검색_성공_시_검색어_기록이_저장된다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of(searchItem(1L, "A")));
                when(linkuSearchHistoryRepository.countByUserId(1L)).thenReturn(5L);

                linkuSearchService.search(1L, "Java", null, 10);

                verify(linkuSearchHistoryRepository).save(any(LinkuSearchHistory.class));
            }

            @Test
            @DisplayName("검색 성공 시 동일 키워드의 기존 기록을 삭제한 뒤 저장한다")
            void 검색_성공_시_동일_키워드_기존_기록을_삭제한_뒤_저장한다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of(searchItem(1L, "A")));
                when(linkuSearchHistoryRepository.countByUserId(1L)).thenReturn(5L);

                linkuSearchService.search(1L, "Java", null, 10);

                InOrder inOrder = inOrder(linkuSearchHistoryRepository);
                inOrder.verify(linkuSearchHistoryRepository).deleteByUserIdAndKeyword(1L, "Java");
                inOrder.verify(linkuSearchHistoryRepository).save(any(LinkuSearchHistory.class));
            }

            @Test
            @DisplayName("검색 후 기록이 10개를 초과하면 가장 오래된 항목을 삭제한다")
            void 검색_기록이_10개_초과_시_가장_오래된_항목을_삭제한다() {
                LinkuSearchHistory oldest = searchHistory(1L, 1L, "오래된키워드");
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of(searchItem(1L, "A")));
                when(linkuSearchHistoryRepository.countByUserId(1L)).thenReturn(11L);
                when(linkuSearchHistoryRepository.findFirstByUserIdOrderByCreatedAtAsc(1L))
                        .thenReturn(Optional.of(oldest));

                linkuSearchService.search(1L, "Java", null, 10);

                verify(linkuSearchHistoryRepository).deleteById(1L);
            }

            @Test
            @DisplayName("검색 후 기록이 10개 이하면 오래된 항목을 삭제하지 않는다")
            void 검색_기록이_10개_이하면_삭제하지_않는다() {
                when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                        .thenReturn(List.of(searchItem(1L, "A")));
                when(linkuSearchHistoryRepository.countByUserId(1L)).thenReturn(10L);

                linkuSearchService.search(1L, "Java", null, 10);

                verify(linkuSearchHistoryRepository, never()).findFirstByUserIdOrderByCreatedAtAsc(anyLong());
                verify(linkuSearchHistoryRepository, never()).deleteById(anyLong());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("keyword가 null이면 예외를 발생시킨다")
            void keyword가_null이면_예외를_발생시킨다() {
                assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, null))
                        .isInstanceOf(GeneralException.class);
                assertThatThrownBy(() -> linkuSearchService.search(1L, null, null, 10))
                        .isInstanceOf(GeneralException.class);
            }

            @Test
            @DisplayName("keyword가 공백 문자열이면 예외를 발생시킨다")
            void keyword가_공백이면_예외를_발생시킨다() {
                assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, "   "))
                        .isInstanceOf(GeneralException.class);
                assertThatThrownBy(() -> linkuSearchService.search(1L, "   ", null, 10))
                        .isInstanceOf(GeneralException.class);
            }

            @Test
            @DisplayName("keyword가 trim 후 1글자면 예외를 발생시킨다")
            void keyword가_trim_후_1글자면_예외를_발생시킨다() {
                assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, " J "))
                        .isInstanceOf(GeneralException.class);
                assertThatThrownBy(() -> linkuSearchService.search(1L, " J ", null, 10))
                        .isInstanceOf(GeneralException.class);
            }
        }
    }

    @Nested
    @DisplayName("최근 검색어 조회 (getRecentKeywords)")
    class GetRecentKeywords {

        @Test
        @DisplayName("검색어 목록을 최신순 DTO로 반환한다")
        void 검색어_목록을_최신순_DTO로_반환한다() {
            given(linkuSearchHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(List.of(
                            searchHistory(2L, 1L, "코틀린"),
                            searchHistory(1L, 1L, "자바")
                    ));

            List<LinkuSearchHistoryItemDTO> result = linkuSearchService.getRecentKeywords(1L);

            assertThat(result).hasSize(2);
            assertEquals(2L, result.get(0).searchHistoryId());
            assertEquals("코틀린", result.get(0).keyword());
            assertEquals(1L, result.get(1).searchHistoryId());
            assertEquals("자바", result.get(1).keyword());
        }

        @Test
        @DisplayName("검색어가 없으면 빈 리스트를 반환한다")
        void 검색어가_없으면_빈_리스트를_반환한다() {
            given(linkuSearchHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(List.of());

            List<LinkuSearchHistoryItemDTO> result = linkuSearchService.getRecentKeywords(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("검색어 단일 삭제 (deleteKeyword)")
    class DeleteKeyword {

        @Test
        @DisplayName("존재하는 검색어 ID로 요청 시 삭제된다")
        void 존재하는_검색어_ID로_요청_시_삭제된다() {
            LinkuSearchHistory history = searchHistory(1L, 1L, "자바");
            given(linkuSearchHistoryRepository.findByUserIdAndId(1L, 1L))
                    .willReturn(Optional.of(history));

            linkuSearchService.deleteKeyword(1L, 1L);

            verify(linkuSearchHistoryRepository).delete(history);
        }

        @Test
        @DisplayName("존재하지 않는 검색어 ID로 요청 시 예외를 던진다")
        void 존재하지_않는_검색어_ID로_요청_시_예외를_던진다() {
            given(linkuSearchHistoryRepository.findByUserIdAndId(1L, 99L))
                    .willReturn(Optional.empty());

            GeneralException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    GeneralException.class,
                    () -> linkuSearchService.deleteKeyword(1L, 99L)
            );
            assertEquals(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND, ex.getCode());
        }
    }

    @Nested
    @DisplayName("검색어 전체 삭제 (deleteAllKeywords)")
    class DeleteAllKeywords {

        @Test
        @DisplayName("검색어가 존재하면 모두 삭제된다")
        void 검색어가_존재하면_모두_삭제된다() {
            linkuSearchService.deleteAllKeywords(1L);

            verify(linkuSearchHistoryRepository).deleteAllByUserId(1L);
        }

        @Test
        @DisplayName("검색어가 없어도 예외 없이 정상 처리된다")
        void 검색어가_없어도_예외_없이_정상_처리된다() {
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> linkuSearchService.deleteAllKeywords(1L)
            );
            verify(linkuSearchHistoryRepository).deleteAllByUserId(1L);
        }
    }
}
