package com.umc.linkyou.service.curation;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.common.KeywordNameResolver;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurationServiceImplTest {
    private static final long USER_ID = 48L;
    private static final long OTHER_USER_ID = 52L;
    private static final long UNKNOWN_USER_ID = 999L;
    private static final long CURATION_ID = 181L;
    private static final long UNKNOWN_CURATION_ID = 999L;

    @InjectMocks
    private CurationServiceImpl service;

    @Mock private UserRepository userRepository;
    @Mock private CurationRepository curationRepository;
    @Mock private KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    @Mock private KeywordNameResolver keywordNameResolver;

    // generateCurationForUser
    @Test
    @DisplayName("해당 월 큐레이션이 없으면 새로 생성한다")
    void generateCurationForUser_creates_whenNotExists() {
        Users user = Users.builder().id(USER_ID).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(curationRepository.existsByUserAndBaseMonth(user, "2026-03")).thenReturn(false);

        Curation saved = Curation.builder().user(user).baseMonth("2026-03").build();
        when(curationRepository.save(any())).thenReturn(saved);

        try (MockedStatic<TransactionSynchronizationManager> tsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
               .thenAnswer(inv -> null);

            service.generateCurationForUser(USER_ID, "2026-03");
        }

        verify(curationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("해당 월 큐레이션이 이미 존재하면 저장하지 않는다")
    void generateCurationForUser_skips_whenAlreadyExists() {
        Users user = Users.builder().id(USER_ID).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(curationRepository.existsByUserAndBaseMonth(user, "2026-03")).thenReturn(true);

        service.generateCurationForUser(USER_ID, "2026-03");

        verify(curationRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 userId로 호출하면 예외가 발생한다")
    void generateCurationForUser_throws_whenUserNotFound() {
        when(userRepository.findById(UNKNOWN_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateCurationForUser(UNKNOWN_USER_ID,
                "2026-03")).isInstanceOf(GeneralException.class);
    }

    // getCurationDetail
    @Test
    @DisplayName("본인 큐레이션을 조회하면 상세 정보를 반환한다")
    void getCurationDetail_success() {
        Users user = Users.builder().id(USER_ID).build();
        Curation curation = Curation.builder()
                .user(user)
                .baseMonth("2026-03")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(curation));
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonth(
                eq(USER_ID), eq("2026-03"), any()))
                .thenReturn(List.of(
                        KeywordMonthlyCount.builder().type(KeywordType.EMOTION).refId(1L).count(5).baseMonth("2026-03").build()
                ));
        when(keywordNameResolver.resolve(KeywordType.EMOTION, 1L)).thenReturn("즐거움");

        CurationDetailResponse result = service.getCurationDetail(USER_ID, CURATION_ID);

        assertThat(result.getTopTags()).containsExactly("즐거움");
        assertThat(result.isMentReady()).isFalse(); // headerMent, footerMent 모두 null
    }

    @Test
    @DisplayName("타인의 큐레이션을 조회하면 FORBIDDEN 예외가 발생한다")
    void getCurationDetail_throws_forbidden_whenNotOwner() {
        Users caller = Users.builder().id(USER_ID).build();
        Users owner = Users.builder().id(OTHER_USER_ID).build();
        Curation curation = Curation.builder()
                .user(owner)
                .baseMonth("2026-03")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(caller));
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(curation));

        assertThatThrownBy(() -> service.getCurationDetail(USER_ID, CURATION_ID)) // 다른 userId
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("존재하지 않는 curationId를 조회하면 NOT_FOUND 예외가 발생한다")
    void getCurationDetail_throws_notFound_whenNotExists() {
        Users user = Users.builder().id(USER_ID).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(curationRepository.findById(UNKNOWN_CURATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurationDetail(USER_ID, UNKNOWN_CURATION_ID))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("headerMent와 footerMent가 모두 있으면 mentReady가 true다")
    void getCurationDetail_mentReady_true_whenBothMentsExist() {
        Users user = Users.builder().id(USER_ID).build();
        Curation curation = Curation.builder()
                .user(user)
                .baseMonth("2026-03")
                .build();
        curation.updateMent("header", "footer");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(curation));
        when(keywordMonthlyCountRepository.findTopByUserIdAndBaseMonth(
                eq(USER_ID), eq("2026-03"), any()))
                .thenReturn(List.of());

        CurationDetailResponse result = service.getCurationDetail(USER_ID, CURATION_ID);

        assertThat(result.isMentReady()).isTrue();
    }
}
