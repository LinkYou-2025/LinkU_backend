package com.umc.linkyou.repository.mapping;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.curationLinkuRepository.UsersLinkuRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersLinkuRepository  extends JpaRepository<UsersLinku, Long> {
    Optional<UsersLinku> findByUserIdAndLinku_Linku(Long userId, String url);

    Optional<UsersLinku> findByUserAndLinku(Users user, Linku linku);

    List<UsersLinku> findByUser_IdAndLinku_LinkuId(Long userId, Long linkuId);

    List<UsersLinku> findByUser_Id(Long userId);

    List<UsersLinku> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("UPDATE UsersLinku ul SET ul.viewCount = ul.viewCount + 1, ul.lastViewedAt = :viewedAt WHERE ul.userLinkuId = :id")
    void incrementViewCount(Long id, LocalDateTime viewedAt);

    List<UsersLinku> findTop10ByUser_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(Long userId);

    List<UsersLinku> findByUser_IdAndLastViewedAtIsNull(Long userId);
}
