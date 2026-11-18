package com.umc.linkyou.repository.classification.domainRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.umc.linkyou.domain.classification.Domain;

public interface DomainRepository extends JpaRepository<Domain, Long>, DomainRepositoryCustom {

    // 대상이 몰려도 크론 한 번이 길어지지 않게 200개까지만 가져오고 나머지는 다음 실행으로 넘긴다.
    @Query(
            value =
                    """
            SELECT d.domain_id
            FROM domains d
            WHERE d.source = 'AUTO'
              AND d.image_url IS NULL
              AND (d.favicon_next_attempt_at IS NULL OR d.favicon_next_attempt_at <= now())
            ORDER BY d.domain_id
            LIMIT 200
            """,
            nativeQuery = true)
    List<Long> findAutoDomainIdsMissingFavicon();
}
