package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LinkuRepository extends JpaRepository<Linku, Long>, LinkuRepositoryCustom {

    @Modifying
    @Query("UPDATE Linku l SET l.totalViewCount = l.totalViewCount + 1 WHERE l.linkuId = :linkuId")
    void incrementTotalViewCount(Long linkuId);
}
