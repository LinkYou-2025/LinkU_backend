package com.umc.linkyou.service.curation;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.CurationSectionInfo;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.curationRepository.CurationSectionInfoRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.CurationFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurationService 단위 테스트")
class CurationServiceTest {
    @InjectMocks private CurationServiceImpl curationService;

    @Mock private UserRepository userRepository;
    @Mock private CurationRepository curationRepository;
    @Mock private CurationSectionInfoRepository curationSectionInfoRepository;

    @Nested
    @DisplayName("큐레이션 생성")
    class GenerateCurationForUser {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("해당 월 큐레이션이 없으면 생성하고 true를 반환한다")
            void 중복없음_저장성공() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(curationRepository.existsByUserAndBaseMonth(user, MONTH)).willReturn(false);
                given(curationRepository.save(any())).willReturn(curation(user, MONTH));

                boolean result;
                try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                    tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).thenAnswer(inv -> null);

                    result = curationService.generateCurationForUser(USER_ID, MONTH);
                }

                assertThat(result).isTrue();
                verify(curationRepository).save(any());
            }

            @Test
            @DisplayName("이미 존재하면 저장하지 않고 false를 반환한다")
            void 이미존재_저장스킵() {
                Users user = user();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(curationRepository.existsByUserAndBaseMonth(user, MONTH)).willReturn(true);

                boolean result = curationService.generateCurationForUser(USER_ID, MONTH);

                assertThat(result).isFalse();
                verify(curationRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 유저면 _USER_NOT_FOUND를 던진다")
            void 생성시_유저없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> curationService.generateCurationForUser(USER_ID, MONTH))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("큐레이션 상세 조회(getCurationDetail)")
    class GetCurationDetail {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("본인 큐레이션이면 상세 정보를 반환한다")
            void 본인큐레이션_조회성공() {
                Users user = user();
                Curation curation = curation(user, MONTH);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(curationRepository.findById(CURATION_ID)).willReturn(Optional.of(curation));

                CurationDetailResponse result = curationService.getCurationDetail(USER_ID, CURATION_ID);

                assertThat(result.getMonth()).isEqualTo(MONTH);
                assertThat(result.isMentReady()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 유저면 _USER_NOT_FOUND를 던진다")
            void 조회시_유저없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> curationService.getCurationDetail(USER_ID, CURATION_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }

            @Test
            @DisplayName("큐레이션이 없으면 _CURATION_NOT_FOUND를 던진다")
            void 큐레이션없음_예외() {
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
                given(curationRepository.findById(CURATION_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> curationService.getCurationDetail(USER_ID, CURATION_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(CurationErrorStatus._CURATION_NOT_FOUND));
            }

            @Test
            @DisplayName("타인의 큐레이션이면 _CURATION_FORBIDDEN을 던진다")
            void 타인권한없음_예외() {
                Users caller = user();
                Users owner = Users.builder().id(OTHER_USER_ID).build();
                Curation curation = curation(owner, MONTH);

                given(userRepository.findById(USER_ID)).willReturn(Optional.of(caller));
                given(curationRepository.findById(CURATION_ID)).willReturn(Optional.of(curation));

                assertThatThrownBy(() -> curationService.getCurationDetail(USER_ID, CURATION_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(CurationErrorStatus._CURATION_FORBIDDEN));
            }
        }
    }

    @Nested
    @DisplayName("연도별 큐레이션 목록(getCurationList)")
    class GetCurationList {
        @Test
        @DisplayName("2025년 미만이면 _CURATION_INVALID_YEAR를 던진다")
        void 연도범위벗어남_예외() {
            assertThatThrownBy(() -> curationService.getCurationList(USER_ID, 2024))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                            .isEqualTo(CurationErrorStatus._CURATION_INVALID_YEAR));
        }

        @Test
        @DisplayName("존재하는 달은 채워서, 없는 달은 빈 채로 12개를 반환한다")
        void 일부월채움_나머지빈값반환() {
            Users user = user();
            Curation curation = Curation.builder().user(user).baseMonth("2026-03").build();

            given(curationRepository.findAllByUserIdAndYear(USER_ID, "2026")).willReturn(List.of(curation));
            given(curationSectionInfoRepository.findByMonthAndSectionNumber("2026-03", 1))
                    .willReturn(Optional.of(CurationSectionInfo.builder().imageUrl("url").build()));

            List<CurationListResponse> result = curationService.getCurationList(USER_ID, 2026);

            assertThat(result).hasSize(12);
        }
    }
}
