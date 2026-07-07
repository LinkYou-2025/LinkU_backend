package com.umc.linkyou.service.tag;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.common.TagNameResolver;
import com.umc.linkyou.web.dto.tag.MyTagRankResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 단위 테스트")
class TagServiceTest {

    private static final long USER_ID = 48L;
    private static final long UNKNOWN_USER_ID = 999L;
    private static final String BASE_MONTH = "2026-03";
    private static final LocalDateTime MONTH_START = YearMonth.parse(BASE_MONTH).atDay(1).atStartOfDay();
    private static final LocalDateTime MONTH_END = YearMonth.parse(BASE_MONTH).plusMonths(1).atDay(1).atStartOfDay();

    @InjectMocks private TagServiceImpl tagService;

    @Mock private UserRepository userRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private TagNameResolver tagNameResolver;

    @Nested
    @DisplayName("getMyTopTags")
    class GetMyTopTags {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("이번_달_저장한_감정과_상황_카운트를_합산해_전체_합계_대비_퍼센트로_반환한다")
            void 이번_달_저장한_감정과_상황_카운트를_합산해_전체_합계_대비_퍼센트로_반환한다() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(Users.builder().id(USER_ID).build()));
                given(usersLinkuRepository.countByEmotionForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.<Object[]>of(
                                new Object[]{1L, 5L},
                                new Object[]{2L, 2L}
                        ));
                given(usersLinkuRepository.countBySituationForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.<Object[]>of(
                                new Object[]{10L, 3L}
                        ));
                given(tagNameResolver.resolve(KeywordType.EMOTION, 1L)).willReturn("즐거움");
                given(tagNameResolver.resolve(KeywordType.SITUATION, 10L)).willReturn("출근길");
                given(tagNameResolver.resolve(KeywordType.EMOTION, 2L)).willReturn("슬픔");

                List<MyTagRankResponse> result = tagService.getMyTopTags(USER_ID, BASE_MONTH, 3);

                assertThat(result).hasSize(3);
                assertThat(result.get(0).getName()).isEqualTo("즐거움");
                assertThat(result.get(0).getPercent()).isEqualTo(50);
                assertThat(result.get(1).getName()).isEqualTo("출근길");
                assertThat(result.get(1).getPercent()).isEqualTo(30);
                assertThat(result.get(2).getName()).isEqualTo("슬픔");
                assertThat(result.get(2).getPercent()).isEqualTo(20);
            }

            @Test
            @DisplayName("종류가_limit보다_많을_시_상위_limit개만_반환한다")
            void 종류가_limit보다_많을_시_상위_limit개만_반환한다() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(Users.builder().id(USER_ID).build()));
                given(usersLinkuRepository.countByEmotionForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.<Object[]>of(
                                new Object[]{1L, 5L},
                                new Object[]{2L, 2L}
                        ));
                given(usersLinkuRepository.countBySituationForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.<Object[]>of(
                                new Object[]{10L, 3L}
                        ));
                given(tagNameResolver.resolve(KeywordType.EMOTION, 1L)).willReturn("즐거움");

                List<MyTagRankResponse> result = tagService.getMyTopTags(USER_ID, BASE_MONTH, 1);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getName()).isEqualTo("즐거움");
                assertThat(result.get(0).getPercent()).isEqualTo(50);
            }

            @Test
            @DisplayName("이번_달_저장한_기록이_없을_시_빈_목록을_반환한다")
            void 이번_달_저장한_기록이_없을_시_빈_목록을_반환한다() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(Users.builder().id(USER_ID).build()));
                given(usersLinkuRepository.countByEmotionForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.of());
                given(usersLinkuRepository.countBySituationForUserAndPeriod(USER_ID, MONTH_START, MONTH_END))
                        .willReturn(List.of());

                List<MyTagRankResponse> result = tagService.getMyTopTags(USER_ID, BASE_MONTH, 3);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_userId로_호출_시_USER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_userId로_호출_시_USER_NOT_FOUND_예외를_던진다() {
                given(userRepository.findById(UNKNOWN_USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> tagService.getMyTopTags(UNKNOWN_USER_ID, BASE_MONTH, 3))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }
        }
    }
}
