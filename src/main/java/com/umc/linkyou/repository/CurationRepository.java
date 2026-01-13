package com.umc.linkyou.repository;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface CurationRepository extends JpaRepository<Curation, Long> {
    boolean existsByUserAndMonth(Users user, String month);
    Optional<Curation> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    // 내 큐레이션 히스토리 조회 (최신 월 순서)
    Page<Curation> findAllByUserOrderByMonthDesc(Users user, Pageable pageable);
}