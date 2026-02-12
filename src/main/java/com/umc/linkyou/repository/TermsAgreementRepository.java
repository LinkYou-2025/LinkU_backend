package com.umc.linkyou.repository;

import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.enums.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
    List<TermsAgreement> findByUserId(Long id);

    Optional<TermsAgreement> findByUserIdAndTermsType(Long id, TermsType termsType);
}
