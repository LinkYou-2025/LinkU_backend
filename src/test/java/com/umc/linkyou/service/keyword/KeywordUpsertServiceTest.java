package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordUpsertService 단위 테스트")
class KeywordUpsertServiceTest {

    @InjectMocks private KeywordUpsertService keywordUpsertService;

    @Mock private KeywordRepository keywordRepository;

    private static final String NAME = "스프링";

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("insertIgnore로_저장을_시도한_뒤_이름으로_조회하여_반환한다")
            void insertIgnore로_저장을_시도한_뒤_이름으로_조회하여_반환한다() {
                Keyword keyword = Keyword.builder().id(1L).name(NAME).build();
                given(keywordRepository.findByName(NAME)).willReturn(Optional.of(keyword));

                Keyword result = keywordUpsertService.upsert(NAME);

                assertThat(result.getName()).isEqualTo(NAME);
                verify(keywordRepository).insertIgnore(NAME);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("insertIgnore_이후에도_조회되지_않으면_KEYWORD_NOT_FOUND를_던진다")
            void insertIgnore_이후에도_조회되지_않으면_KEYWORD_NOT_FOUND를_던진다() {
                given(keywordRepository.findByName(NAME)).willReturn(Optional.empty());

                assertThatThrownBy(() -> keywordUpsertService.upsert(NAME))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(LinkuErrorStatus._KEYWORD_NOT_FOUND));
            }
        }
    }
}
