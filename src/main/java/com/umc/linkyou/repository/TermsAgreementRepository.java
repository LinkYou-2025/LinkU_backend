package com.umc.linkyou.repository;

import com.querydsl.core.Fetchable;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.enums.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
    List<TermsAgreement> findByUserId(Long id);
    List<TermsAgreement> findAllByUserId(Long userId);

    Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType);

    void deleteAllByUserIdIn(List<Long> userIds);
}
