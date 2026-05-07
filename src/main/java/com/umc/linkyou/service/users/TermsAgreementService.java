package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.TermsConverter;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsAgreementService {
    private final UsersUtils usersUtils;
    private final TermsAgreementRepository termsAgreementRepository;
    private final UserRepository userRepository;

    /**
     * DTO로 전달받은 약관 리스트를 일괄적으로 처리 (Update or Insert)
     */
    // 1. Controller에서 호출하는 용도 (userDetails 기반)
    @Transactional
    public UserResponseDTO.TermsStatusDTO updateTermsAgree(CustomUserDetails userDetails, UserRequestDTO.TermsAgreeDTO request) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        upsertTerms(user, request.getTermsMap()); // 공통 로직 호출

        List<TermsAgreement> updatedList = termsAgreementRepository.findAllByUserId(userId);
        return TermsConverter.toTermsStatusDTO(userId, updatedList);
    }

    // 2. UserService 등 내부 서비스에서 직접 유저 객체로 호출하는 용도 (Upsert 공통 로직)
    @Transactional
    public void upsertTerms(Users user, Map<String, Boolean> termsMap) {
        if (termsMap == null || termsMap.isEmpty()) return;

        Map<TermsType, TermsAgreement> existingMap = termsAgreementRepository.findAllByUserId(user.getId()).stream()
                .collect(Collectors.toMap(TermsAgreement::getTermsType, a -> a));

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
        Users user = usersUtils.validateUser(userDetails);

        List<TermsAgreement> agreements = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), agreements);
    }
}
