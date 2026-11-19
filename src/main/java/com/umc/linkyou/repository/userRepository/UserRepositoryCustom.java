package com.umc.linkyou.repository.userRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.umc.linkyou.domain.Users;

public interface UserRepositoryCustom {
    List<Long> findInactiveUserIds(LocalDateTime beforeDateTime);

    Optional<Users> findNotInactiveUserById(Long userId);
}
