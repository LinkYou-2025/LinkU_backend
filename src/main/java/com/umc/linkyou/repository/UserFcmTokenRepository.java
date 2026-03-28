package com.umc.linkyou.repository;

import com.umc.linkyou.domain.UsersFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFcmTokenRepository extends JpaRepository<UsersFcmToken, Long> {
    UsersFcmToken findByUser_IdAndFcmToken(Long userId, String newToken);

    List<UsersFcmToken> findAllByUser_IdAndIsActiveTrue(Long userId);

    List<UsersFcmToken> findAllByIsActiveTrue();
}
