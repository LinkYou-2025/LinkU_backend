package com.umc.linkyou.repository.userRepository;

import com.umc.linkyou.domain.Users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    List<Long> findInactiveUserIds(LocalDateTime beforeDateTime);
    Optional<Users> findNotInactiveUserById(Long userId);
}
