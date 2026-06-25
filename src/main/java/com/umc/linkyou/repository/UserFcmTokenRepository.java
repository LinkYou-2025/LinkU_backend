package com.umc.linkyou.repository;

import com.umc.linkyou.domain.UsersFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserFcmTokenRepository extends JpaRepository<UsersFcmToken, Long> {

    UsersFcmToken findByUser_IdAndFcmToken(Long userId, String newToken);

    @Query("SELECT t FROM UsersFcmToken t WHERE t.user.id = :userId AND t.isActive = true AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<UsersFcmToken> findAllActiveAndNotExpiredByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM UsersFcmToken t WHERE t.isActive = true AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<UsersFcmToken> findAllActiveAndNotExpired(@Param("now") LocalDateTime now);

}
