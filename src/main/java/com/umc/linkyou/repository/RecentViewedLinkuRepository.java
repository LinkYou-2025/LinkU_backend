package com.umc.linkyou.repository;

import com.umc.linkyou.domain.RecentViewedLinku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentViewedLinkuRepository extends JpaRepository<RecentViewedLinku, Long> {
    Optional<RecentViewedLinku> findByUser_IdAndLinku_LinkuId(Long userId, Long linkuId);
    List<RecentViewedLinku> findAllByUser_IdOrderByViewedAtDesc(Long userId);
    List<RecentViewedLinku> findTop10ByUser_IdOrderByViewedAtDesc(Long userId);
    void deleteByUserIdAndLinkuId(Long userId, Long linkuId);

}
