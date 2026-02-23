package com.umc.linkyou.converter;

import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.web.dto.UserRequestDTO;
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
    //List<TermsAgreement> ->  Map<String, Boolean> 변환; key value형태로 변환해서 반환
    public static Map<String, Boolean> toTermsStatusMap(List<TermsAgreement> agreements) {
        return agreements.stream() //순서대로 계산
                .collect(Collectors.toMap(
                        agreement -> agreement.getTermsType().name(), //Key: "TERMS_OF_USE"
                        TermsAgreement::getIsAgreed,  // Value: true/false
                        (existing, replacement)->replacement, //이미 같은 TermsType으로 row가 있으면 최신값으로 변경
                        LinkedHashMap::new
                ));  // result: {"TERMS_OF_USE":true, "MARKETING":false}
    }
    //TermsStatus response 전체 빌드
    public static UserResponseDTO.TermsStatusDTO toTermsStatusDTO(Long userId, List<TermsAgreement> agreements) {
        Map<String, Boolean> termsStatus = toTermsStatusMap(agreements);
        List<TermsAgreement> requiredList = agreements.stream()
                .filter(TermsAgreement::getIsRequired)
                .toList();
        boolean allRequiredAgreed = !requiredList.isEmpty()
                && requiredList.stream().allMatch(TermsAgreement::getIsAgreed);
        return UserResponseDTO.TermsStatusDTO.builder()
                .userId(userId)
                .termsStatus(termsStatus)
                .allRequiredAgreed(allRequiredAgreed) //필수 완료 여부
                .build();
    }

    // 전체 동의여부 TermsAgreeDTO  → List<TermsAgreement> 변환
    public static List<TermsAgreement> toTermsAgreements(Users user, UserRequestDTO.TermsAgreeDTO request) {
        return request.getTermsTypes().stream()
                .map(termsTypeStr -> {
                    TermsType termsType = TermsType.fromString(termsTypeStr);
                    return TermsAgreement.builder()
                            .user(user)
                            .termsType(termsType)
                            .isRequired(TermsConverter.isRequiredTerms(termsType))
                            .termsVersion(request.getTermsVersion())
                            .agreedAt(LocalDateTime.now())
                            .isAgreed(true)
                            .build();
                })
                .toList();
    }

    // 개별 약관 상태 확인
    private static boolean isRequiredTerms(TermsType termsType) {
        return switch (termsType) {
            case TERMS_OF_USE, PRIVACY_POLICY -> true;
            case MARKETING -> false;
        };
    }
    public static TermsAgreement toSingleTermAgreement(Users user, String termsTypeStr, boolean isAgreed) {
        TermsType termsType = TermsType.fromString(termsTypeStr);
        return TermsAgreement.builder()
                .user(user)
                .termsType(termsType)
                .isRequired(isRequiredTerms(termsType))
                .termsVersion(CURRENT_TERMS_VERSION)  // 기본 버전
                .agreedAt(LocalDateTime.now())
                .isAgreed(isAgreed)
                .build();
    }
    //기존 레코드 업데이트
    public static void updateAgreement(TermsAgreement agreement, boolean isAgreed) {
        agreement.setIsAgreed(isAgreed);
        agreement.setAgreedAt(LocalDateTime.now());
    }
}
