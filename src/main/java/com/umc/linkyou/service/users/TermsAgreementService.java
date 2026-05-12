package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
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

    @Transactional
    public UserResponseDTO.TermsStatusDTO termsAgreeBatch(@Valid UserRequestDTO.TermsAgreeDTO request, Long userId) {
        Users user = loadActiveUser(userId);

        Map<TermsType, TermsAgreement> existingMap = termsAgreementRepository.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(
                        TermsAgreement::getTermsType,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<TermsAgreement> toSave = TermsConverter.toTermsAgreements(user, request)
                .stream()
                .map(incoming -> {
                    TermsAgreement existing = existingMap.get(incoming.getTermsType());
                    if (existing != null) {
                        TermsConverter.updateAgreement(existing, incoming.getIsAgreed());
                        return existing;
                    }
                    return incoming;
                }).toList();
        termsAgreementRepository.saveAll(toSave);

        List<TermsAgreement> saved = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), saved);
    }

    @Transactional
    public UserResponseDTO.TermsStatusDTO updateTermsAgree(Long userId, @NotNull String termsTypeStr, Boolean isAgreed) {
        Users user = loadActiveUser(userId);
        TermsType termsType = TermsType.fromString(termsTypeStr);

        Optional<TermsAgreement> existing = termsAgreementRepository.findByUserIdAndTermsType(user.getId(), termsType);

        TermsAgreement agreement;
        if (existing.isPresent()) {
            agreement = existing.get();
            TermsConverter.updateAgreement(agreement, isAgreed);
        } else {
            agreement = TermsConverter.toSingleTermAgreement(user, termsTypeStr, isAgreed);
        }
        termsAgreementRepository.save(agreement);

        List<TermsAgreement> updated = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), updated);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.TermsStatusDTO getTermsStatus(Long userId) {
        Users user = loadActiveUser(userId);
        List<TermsAgreement> agreements = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), agreements);
    }

    private Users loadActiveUser(Long userId) {
        return userRepository.findNotInactiveUserById(userId)
                .orElseThrow(() -> {
                    if (!userRepository.existsById(userId)) {
                        return new UserHandler(UserErrorStatus._USER_NOT_FOUND);
                    }
                    return new UserHandler(UserErrorStatus._USER_INACTIVE);
                });
    }
}
