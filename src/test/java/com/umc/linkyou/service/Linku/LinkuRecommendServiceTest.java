package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.recommend.UserContentProfileRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuRecommendService 단위 테스트")
class LinkuRecommendServiceTest {

    private static final long USER_ID = 48L;
    private static final long SITUATION_ID = 3L;
    private static final long EMOTION_ID = 5L;

    @InjectMocks private LinkuRecommendService linkuRecommendService;

    @Mock private EmotionRepository emotionRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private UserRepository userRepository;
    @Mock private SituationRepository situationRepository;
    @Mock private SituationJobRepository situationJobRepository;
    @Mock private LinkuViewService linkuViewService;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private SituationCategoryService situationCategoryService;
    @Mock private UserContentProfileRepository userContentProfileRepository;
    @Mock private RecommendScoreProperties recommendScoreProperties;

    @Nested
    @DisplayName("recommendLinku")
    class RecommendLinku {

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("job이_설정되지_않은_TEMP_상태_유저가_호출_시_JOB_NOT_SET_예외를_던진다")
            void job이_설정되지_않은_TEMP_상태_유저가_호출_시_JOB_NOT_SET_예외를_던진다() {
                Users tempUser = Users.builder().id(USER_ID).job(null).build();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(tempUser));
                given(emotionRepository.findById(EMOTION_ID))
                        .willReturn(Optional.of(Emotion.builder().emotionId(EMOTION_ID).name("행복").build()));
                given(situationRepository.findById(SITUATION_ID))
                        .willReturn(Optional.of(Situation.builder().id(SITUATION_ID).name("공부").build()));

                assertThatThrownBy(() -> linkuRecommendService.recommendLinku(USER_ID, SITUATION_ID, EMOTION_ID, null, 5))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._JOB_NOT_SET));
            }
        }
    }
}
