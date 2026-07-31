package com.umc.linkyou.repository;

import com.umc.linkyou.domain.LinkuSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkuSearchHistoryRepository extends JpaRepository<LinkuSearchHistory, Long> {

    Optional<LinkuSearchHistory> findByUserIdAndId(Long userId, Long id);

    List<LinkuSearchHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    Optional<LinkuSearchHistory> findFirstByUserIdOrderByCreatedAtAsc(Long userId);

    void deleteByUserIdAndKeyword(Long userId, String keyword);

    void deleteAllByUserId(Long userId);
}
