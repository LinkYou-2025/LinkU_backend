package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.converter.TermsConverter;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.umc.linkyou.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsAgreementService {

    private final TermsAgreementRepository termsAgreementRepository;
    private final UserRepository userRepository;
    private final UserStatusValidator userStatusValidator;

    /**
     * DTO로 전달받은 약관 리스트를 일괄적으로 처리 (Update or Insert)
     */
    // 1. Controller에서 호출하는 용도 (userDetails 기반)
    @Transactional
    public UserResponseDTO.TermsStatusDTO updateTermsAgree(CustomUserDetails userDetails, UserRequestDTO.TermsAgreeDTO request) {
        Users user = userRepository.findById(userDetails.getUserId()) // userDetails.getUserId() 직접 호출
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        userStatusValidator.validateLoginAllowed(user); // UserStatusValidator 활용

        upsertTerms(user, request.termsMap());

        List<TermsAgreement> updatedList = termsAgreementRepository.findAllByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), updatedList);
    }

    /**
     * 약관 Upsert 로직
     */
    public void upsertTerms(Users user, Map<String, Boolean> termsMap) {
        if (termsMap == null || termsMap.isEmpty()) {
            throw new GeneralException(UserErrorStatus.INVALID_TERMS_TYPE);
        }

        Map<TermsType, TermsAgreement> existingMap = termsAgreementRepository.findAllByUserId(user.getId()).stream()
                .collect(Collectors.toMap(
                        TermsAgreement::getTermsType,
                        Function.identity(),
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));

        termsMap.forEach((typeStr, isAgreed) -> {
            TermsType type = TermsType.fromString(typeStr);
            if (existingMap.containsKey(type)) {
                TermsConverter.updateAgreement(existingMap.get(type), isAgreed);
            } else {
                termsAgreementRepository.save(TermsConverter.toSingleTermAgreement(user, typeStr, isAgreed));
            }
        });
    }
    // GET /terms/status - 약관 상태 조회
    @Transactional(readOnly = true)
    public UserResponseDTO.TermsStatusDTO getTermsStatus(CustomUserDetails userDetails) {
        // userDetails.getUserId()를 사용하여 단순 조회
        List<TermsAgreement> agreements = termsAgreementRepository.findAllByUserId(userDetails.getUserId());
        return TermsConverter.toTermsStatusDTO(userDetails.getUserId(), agreements);
    }
}
