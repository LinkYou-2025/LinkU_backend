package com.umc.linkyou.repository;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.UsersFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFcmTokenRepository extends JpaRepository<UsersFcmToken, Long> {
    UsersFcmToken findByUser_IdAndFcmToken(Long userId, String newToken);
}
