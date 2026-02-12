package com.umc.linkyou.service.users;

import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.TermsConverter;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermsAgreementService {
    private final UsersUtils usersUtils;
    private final TermsAgreementRepository termsAgreementRepository;

    private final UserRepository userRepository;
    //전체 동의 여부 반환
    @Transactional
    public UserResponseDTO.TermsStatusDTO termsAgreeBatch(UserRequestDTO.@Valid TermsAgreeDTO request, CustomUserDetails userDetails){
        //1. token이 올바른지, 사용자가 존재하는지, 사용자가 있다면 activated 상태인지
        Users user = usersUtils.validateUser(userDetails, userRepository);
        // DTO → Entity 변환 + 저장
        List<TermsAgreement> agreements = TermsConverter.toTermsAgreements(user, request);
        termsAgreementRepository.saveAll(agreements);

        // Entity → Response 변환
        List<TermsAgreement> saved = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), saved);
    }
}
