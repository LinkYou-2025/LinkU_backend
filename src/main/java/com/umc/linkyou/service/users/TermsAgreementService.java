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
    @Transactional
    public UserResponseDTO.TermsStatusDTO updateTermsAgree(CustomUserDetails userDetails, UserRequestDTO.TermsAgreeDTO request) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        // 1. 기존 데이터 Map 조회
        Map<TermsType, TermsAgreement> existingMap = termsAgreementRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(TermsAgreement::getTermsType, a -> a));

        // 2. 요청받은 Map(타입:상태)을 순회하며 처리
        request.getTermsMap().forEach((typeStr, isAgreed) -> {
            TermsType type = TermsType.fromString(typeStr);

            if (existingMap.containsKey(type)) {
                // [Update] 이미 있으면 넘어온 boolean 값(isAgreed)으로 업데이트
                TermsConverter.updateAgreement(existingMap.get(type), isAgreed);
            } else {
                // [Create] 없으면 새로 생성 (넘어온 boolean 값 적용)
                termsAgreementRepository.save(TermsConverter.toSingleTermAgreement(user, typeStr, isAgreed));
            }
        });

        // 3. 최종 상태 반환
        List<TermsAgreement> updatedList = termsAgreementRepository.findAllByUserId(userId);
        return TermsConverter.toTermsStatusDTO(userId, updatedList);
    }
    // GET /terms/status - 약관 상태 조회
    @Transactional(readOnly = true)
    public UserResponseDTO.TermsStatusDTO getTermsStatus(CustomUserDetails userDetails) {
        Users user = usersUtils.validateUser(userDetails);

        List<TermsAgreement> agreements = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), agreements);
    }
}
