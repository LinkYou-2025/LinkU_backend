package com.umc.linkyou.converter;

import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TermsConverter {

    public static final String CURRENT_TERMS_VERSION = "v1.0";

    /**
     * 전체 약관 상태 응답 DTO 생성
     */
    public static UserResponseDTO.TermsStatusDTO toTermsStatusDTO(Long userId, List<TermsAgreement> agreements) {
        // 1. 약관 타입별 동의 여부 Map 생성 (순서 보장)
        Map<TermsType, Boolean> termsStatusMap = agreements.stream()
                .collect(Collectors.toMap(
                        TermsAgreement::getTermsType,
                        TermsAgreement::getIsAgreed,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));

        // 2. 필수 약관 리스트 필터링
        List<TermsAgreement> requiredList = agreements.stream()
                .filter(TermsAgreement::getIsRequired)
                .toList();

        // 3. 필수 약관이 존재하고, 모두 동의(true)했는지 확인
        boolean allRequiredAgreed = !requiredList.isEmpty()
                && requiredList.stream().allMatch(TermsAgreement::getIsAgreed);

        return UserResponseDTO.TermsStatusDTO.builder()
                .userId(userId)
                .termsStatus(termsStatusMap)
                .allRequiredAgreed(allRequiredAgreed)
                .build();
    }

    /**
     * 신규 약관 동의 객체 생성 (Insert용)
     */
    public static TermsAgreement toSingleTermAgreement(Users user, TermsType termsType, boolean isAgreed) {
        return TermsAgreement.builder()
                .user(user)
                .termsType(termsType)
                .isRequired(isRequiredTerms(termsType))
                .termsVersion(CURRENT_TERMS_VERSION)
                .agreedAt(LocalDateTime.now())
                .isAgreed(isAgreed)
                .build();
    }

    /**
     * 기존 약관 동의 객체 업데이트 (Update용)
     */
    public static void updateAgreement(TermsAgreement agreement, boolean isAgreed) {
        agreement.setIsAgreed(isAgreed);
        agreement.setAgreedAt(LocalDateTime.now());
    }

    /**
     * 약관 타입별 필수 여부 판단 (비즈니스 로직)
     */
    private static boolean isRequiredTerms(TermsType termsType) {
        return switch (termsType) {
            case TERMS_OF_USE, PRIVACY_POLICY -> true;
            case MARKETING -> false;
        };
    }
}
