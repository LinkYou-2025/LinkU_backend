package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.umc.linkyou.support.fixture.LinkuFixture.quickSearchItem;
import static com.umc.linkyou.support.fixture.LinkuFixture.searchItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("퀵서치 - keyword 앞뒤 공백을 제거한다")
        void quickSearch_trimKeyword() {
            when(linkuRepository.findQuickByKeyword(1L, "Java"))
                    .thenReturn(List.of(quickSearchItem("Java Guide", 1L)));

            List<LinkuQuickSearchResponseDTO> result = linkuSearchService.quickSearch(1L, "  Java  ");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Java Guide");
            verify(linkuRepository).findQuickByKeyword(1L, "Java");
        }

        @Test
        @DisplayName("검색 - size+1개가 조회되면 hasNext true, 마지막 항목은 잘라낸다")
        void search_hasNext() {
            when(linkuRepository.searchUserLinks(1L, "Java", null, 2))
                    .thenReturn(List.of(searchItem(30L, "A"), searchItem(20L, "B"), searchItem(10L, "C")));

            LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                    linkuSearchService.search(1L, "Java", null, 2);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.items()).hasSize(2);
            assertThat(result.nextCursor()).isEqualTo(20L);
        }

        @Test
        @DisplayName("검색 - size 이하로 조회되면 hasNext false")
        void search_noNext() {
            when(linkuRepository.searchUserLinks(1L, "Java", null, 10))
                    .thenReturn(List.of(searchItem(30L, "A")));

            LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                    linkuSearchService.search(1L, "Java", null, 10);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.items()).hasSize(1);
            assertThat(result.nextCursor()).isEqualTo(30L);
        }

        @Test
        @DisplayName("검색 - 결과가 없으면 nextCursor는 null")
        void search_empty() {
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
        void search_zeroSize() {
            LinkuSearchResponseDTO.LinkuSearchCursorPageResponse result =
                    linkuSearchService.search(1L, "Java", null, 0);

            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            verify(linkuRepository, never()).searchUserLinks(anyLong(), anyString(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {

        @Test
        @DisplayName("keyword가 null이면 예외를 발생시킨다")
        void nullKeyword() {
            assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, null))
                    .isInstanceOf(GeneralException.class);
            assertThatThrownBy(() -> linkuSearchService.search(1L, null, null, 10))
                    .isInstanceOf(GeneralException.class);
        }

        @Test
        @DisplayName("keyword가 공백 문자열이면 예외를 발생시킨다")
        void blankKeyword() {
            assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, "   "))
                    .isInstanceOf(GeneralException.class);
            assertThatThrownBy(() -> linkuSearchService.search(1L, "   ", null, 10))
                    .isInstanceOf(GeneralException.class);
        }

        @Test
        @DisplayName("keyword가 trim 후 1글자면 예외를 발생시킨다")
        void tooShortKeyword() {
            assertThatThrownBy(() -> linkuSearchService.quickSearch(1L, " J "))
                    .isInstanceOf(GeneralException.class);
            assertThatThrownBy(() -> linkuSearchService.search(1L, " J ", null, 10))
                    .isInstanceOf(GeneralException.class);
        }
    }
}
