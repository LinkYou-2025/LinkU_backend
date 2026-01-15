package com.umc.linkyou.repository.mapping;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.CurationLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurationLikeRepository extends JpaRepository<CurationLike, Long> {

    boolean existsByUserAndCuration(Users user, Curation curation);

    Optional<CurationLike> findByUserAndCuration(Users user, Curation curation);

    // 좋아요한 큐레이션 top6개
    List<CurationLike> findTop6ByUserOrderByCreatedAtDesc(Users user);

    // 좋아요한 큐레이션 전체 조회 (좋아요 누른 최신순)
    // - N+1 문제 방지를 위해 curation 정보를 같이 가져옴
    @EntityGraph(attributePaths = {"curation"})
    Page<CurationLike> findAllByUserOrderByCreatedAtDesc(Users user, Pageable pageable);
}

