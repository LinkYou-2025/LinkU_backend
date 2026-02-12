package com.umc.linkyou.converter;

import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TermsConverter {
    //List<TermsAgreement> ->  Map<String, Boolean> 변환; key value형태로 변환해서 반환
    public static Map<String, Boolean> toTermsStatusMap(List<TermsAgreement> agreements) {
        return agreements.stream() //순서대로 계산
                .collect(Collectors.toMap(
                        agreement -> agreement.getTermsType().name(), //Key: "TERMS_OF_USE"
                        TermsAgreement::getIsAgreed  // Value: true/false
                ));  // result: {"TERMS_OF_USE":true, "MARKETING":false}
    }
    //TermsStatus response 전체 빌드
    public static UserResponseDTO.TermsStatusDTO toTermsStatusDTO(Long userId, List<TermsAgreement> agreements) {
        Map<String, Boolean> termsStatus = toTermsStatusMap(agreements);
        boolean allRequiredAgreed = agreements.stream()  //필수약관 중 isRequired가 true인 것만 필터링
                .filter(TermsAgreement::getIsRequired)
                .allMatch(TermsAgreement::getIsAgreed); //모두 treu면 true
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
                    TermsType termsType = TermsType.valueOf(termsTypeStr);
                    return TermsAgreement.builder()
                            .user(user)
                            .termsType(termsType)
                            .isRequired(TermsConverter.isRequiredTerms(termsType))
                            .termsVersion(request.getTermsVersion())
                            .agreedAt(java.time.LocalDateTime.now())
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
        TermsType termsType = TermsType.valueOf(termsTypeStr);
        return TermsAgreement.builder()
                .user(user)
                .termsType(termsType)
                .isRequired(isRequiredTerms(termsType))
                .termsVersion("v1.0")  // 기본 버전
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
