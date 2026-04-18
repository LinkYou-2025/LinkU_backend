package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurationRepository extends JpaRepository<Curation, Long> {
    boolean existsByUserAndMonth(Users user, String month);
    Optional<Curation> findTopByUser_IdOrderByMonthDesc(Long userId);

    // 올해 큐레이션 조회
    List<Curation> findAllByUser_IdAndMonthStartingWith(Long userId, String yearPrefix);
}