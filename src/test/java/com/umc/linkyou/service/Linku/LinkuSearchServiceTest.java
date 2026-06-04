package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        @DisplayName("keyword 앞뒤 공백을 제거한다")
        void suggest_trimKeyword() {
            List<LinkuSearchSuggestionResponse> mockResult = List.of(
                    new LinkuSearchSuggestionResponse(1L, "Java Guide", "img", "link1")
            );

            when(linkuRepository.findUserSavedSuggestions(1L, "Java"))
                    .thenReturn(mockResult);

            List<LinkuSearchSuggestionResponse> result = linkuSearchService.suggest(1L, "  Java  ");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Java Guide");
            verify(linkuRepository).findUserSavedSuggestions(1L, "Java");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {

        @Test
        @DisplayName("keyword가 null이면 예외를 발생시킨다")
        void suggest_nullKeyword() {
            assertThatThrownBy(() -> linkuSearchService.suggest(1L, null))
                    .isInstanceOf(GeneralException.class);

            verify(linkuRepository, never()).findUserSavedSuggestions(1L, "");
        }

        @Test
        @DisplayName("keyword가 빈 문자열이면 예외를 발생시킨다")
        void suggest_emptyKeyword() {
            assertThatThrownBy(() -> linkuSearchService.suggest(1L, ""))
                    .isInstanceOf(GeneralException.class);

            verify(linkuRepository, never()).findUserSavedSuggestions(1L, "");
        }

        @Test
        @DisplayName("keyword가 공백 문자열이면 예외를 발생시킨다")
        void suggest_blankKeyword() {
            assertThatThrownBy(() -> linkuSearchService.suggest(1L, "   "))
                    .isInstanceOf(GeneralException.class);

            verify(linkuRepository, never()).findUserSavedSuggestions(1L, "");
        }
    }
}
