package com.umc.linkyou.service.users;

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
    //전체 동의 여부 반환
    @Transactional
    public UserResponseDTO.TermsStatusDTO termsAgreeBatch(UserRequestDTO.@Valid TermsAgreeDTO request, CustomUserDetails userDetails){
        //1. 사용자 검증
        Users user = usersUtils.validateUser(userDetails);

        //2. 기존 약관 동의 목록을 Terms Type  기준으로 매핑
        Map<TermsType,TermsAgreement> existingMap = termsAgreementRepository.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap( //db에 이미 중복된 레코드가 있으면 오래된 거로
                        TermsAgreement::getTermsType,  //TermsType
                        Function.identity(), //TermsAgreement
                        (a, b) -> a, //같은 TermsType이 2번 이상 나오면 오래된 a를 선택, 새로운 b버림
                        LinkedHashMap::new //삽입 순서 유지
                ));
        //3. DTO -> 엔티티 변환 후 upsert(update or insert) 처리
        List<TermsAgreement> toSave = TermsConverter.toTermsAgreements(user, request)
                .stream()
                .map(incoming -> {
                    TermsAgreement existing = existingMap.get(incoming.getTermsType());
                    if(existing != null) { //해당 termstype이 존재하면 update 실행
                        TermsConverter.updateAgreement(existing,incoming.getIsAgreed()); //기존 엔티티의 isAgreed 필드를 새 값으로 덮어씁니다.
                        return existing;
                    }
                    //insert: 신규 엔티티
                    return incoming;
                }).toList(); //스트림 결과를 다시 List<TermsAgreement> 형태로 반환
        termsAgreementRepository.saveAll(toSave);


        // Entity → Response 변환
        List<TermsAgreement> saved = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), saved);
    }

    @Transactional
    public UserResponseDTO.TermsStatusDTO updateTermsAgree(CustomUserDetails userDetails, @NotNull String termsTypeStr, Boolean isAgreed) {
        Users user = usersUtils.validateUser(userDetails);
        TermsType termsType = TermsType.fromString(termsTypeStr);
        //기존 레코그 조회
        Optional<TermsAgreement> existing = termsAgreementRepository.findByUserIdAndTermsType(user.getId(), termsType);

        TermsAgreement agreement;
        if (existing.isPresent()) {
            // 기존 업데이트
            agreement = existing.get();
            TermsConverter.updateAgreement(agreement, isAgreed);
        } else {
            // 신규 생성
            agreement = TermsConverter.toSingleTermAgreement(user, termsTypeStr, isAgreed);
        }
        termsAgreementRepository.save(agreement);

        List<TermsAgreement> updated = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), updated);
    }

    // GET /terms/status - 약관 상태 조회
    @Transactional(readOnly = true)
    public UserResponseDTO.TermsStatusDTO getTermsStatus(CustomUserDetails userDetails) {
        Users user = usersUtils.validateUser(userDetails);

        List<TermsAgreement> agreements = termsAgreementRepository.findByUserId(user.getId());
        return TermsConverter.toTermsStatusDTO(user.getId(), agreements);
    }
}
