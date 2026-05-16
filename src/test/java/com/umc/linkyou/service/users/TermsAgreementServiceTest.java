package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermsAgreementServiceTest {

    @InjectMocks private TermsAgreementService termsAgreementService;
    @Mock private TermsAgreementRepository termsAgreementRepository;

    @Nested
    @DisplayName("upsertTerms (약관 저장/업데이트) 로직")
    class UpsertTerms {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 기존 기록이 없는 약관은 새롭게 저장(save)된다")
            void upsert_new_agreement_success() {
                // given
                Users user = Users.builder().id(1L).build();
                Map<TermsType, Boolean> termsMap = Map.of(TermsType.TERMS_OF_USE, true);

                when(termsAgreementRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                // when
                termsAgreementService.upsertTerms(user, termsMap);

                // then
                verify(termsAgreementRepository, times(1)).save(any(TermsAgreement.class));
            }

            @Test
            @DisplayName("성공 - 기존 기록이 있는 약관은 상태만 업데이트된다")
            void update_existing_agreement_success() {
                // given
                Users user = Users.builder().id(1L).build();
                TermsAgreement existing = TermsAgreement.builder()
                        .termsType(TermsType.MARKETING)
                        .isAgreed(false)
                        .build();

                when(termsAgreementRepository.findAllByUserId(1L)).thenReturn(List.of(existing));
                Map<TermsType, Boolean> termsMap = Map.of(TermsType.MARKETING, true);

                // when
                termsAgreementService.upsertTerms(user, termsMap);

                // then
                assertEquals(true, existing.getIsAgreed());
                verify(termsAgreementRepository, never()).save(any()); // save가 아닌 변경 감지(Dirty Checking) 활용
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 전달된 약관 맵이 비어있으면 INVALID_TERMS_TYPE 예외가 발생한다")
            void fail_when_map_is_empty() {
                // given
                Users user = Users.builder().id(1L).build();
                Map<TermsType, Boolean> emptyMap = Map.of();

                // when & then
                GeneralException exception = assertThrows(GeneralException.class,
                        () -> termsAgreementService.upsertTerms(user, emptyMap));

                assertEquals(UserErrorStatus.INVALID_TERMS_TYPE, exception.getCode());
            }
        }
    }
}
