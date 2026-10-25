package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.user.MarketingAgreeResponseDTO;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermsAgreementServiceTest {

    @InjectMocks private TermsAgreementService termsAgreementService;
    @Mock private TermsAgreementRepository termsAgreementRepository;
    @Mock private UserRepository userRepository;

    @Nested
    @DisplayName("upsertTerms (약관 저장/업데이트) 로직")
    class UpsertTerms {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 기존 기록이 없는 약관은 새롭게 저장(save)된다")
            void 기존_기록이_없는_약관은_새롭게_저장된다() {
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
            void 기존_기록이_있는_약관은_상태만_업데이트된다() {
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
            void 전달된_약관_맵이_비어있으면_INVALID_TERMS_TYPE_예외가_발생한다() {
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

    @Nested
    @DisplayName("마케팅 약관 동의 토글")
    class ToggleMarketing {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("기존 동의 기록이 있으면 상태를 반전한 결과를 반환한다")
            void 기존_동의_기록이_있으면_상태를_반전한_결과를_반환한다() {
                TermsAgreement agreement = TermsAgreement.builder()
                        .termsType(TermsType.MARKETING)
                        .isAgreed(true)
                        .build();
                when(termsAgreementRepository.findByUserIdAndTermsType(1L, TermsType.MARKETING))
                        .thenReturn(Optional.of(agreement));

                MarketingAgreeResponseDTO response = termsAgreementService.toggleMarketing(userDetails());

                assertEquals(false, response.isMarketingAgreed());
                assertEquals(false, agreement.getIsAgreed());
                verify(termsAgreementRepository, never()).save(any(TermsAgreement.class));
            }

            @Test
            @DisplayName("기존 기록이 없으면 동의 상태로 생성한 결과를 반환한다")
            void 기존_기록이_없으면_동의_상태로_생성한_결과를_반환한다() {
                Users user = Users.builder().id(1L).build();
                TermsAgreement createdAgreement = TermsAgreement.builder()
                        .user(user)
                        .termsType(TermsType.MARKETING)
                        .isAgreed(true)
                        .build();
                when(termsAgreementRepository.findByUserIdAndTermsType(1L, TermsType.MARKETING))
                        .thenReturn(Optional.empty());
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(termsAgreementRepository.save(any(TermsAgreement.class))).thenReturn(createdAgreement);

                MarketingAgreeResponseDTO response = termsAgreementService.toggleMarketing(userDetails());

                assertEquals(true, response.isMarketingAgreed());
                verify(termsAgreementRepository).save(any(TermsAgreement.class));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("기존 기록이 없고 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
            void 기존_기록이_없고_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
                when(termsAgreementRepository.findByUserIdAndTermsType(1L, TermsType.MARKETING))
                        .thenReturn(Optional.empty());
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                GeneralException exception = assertThrows(
                        GeneralException.class,
                        () -> termsAgreementService.toggleMarketing(userDetails()));

                assertEquals(UserErrorStatus._USER_NOT_FOUND, exception.getCode());
                verify(termsAgreementRepository, never()).save(any(TermsAgreement.class));
            }
        }
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(1L, Role.USER, "kakao");
    }
}
