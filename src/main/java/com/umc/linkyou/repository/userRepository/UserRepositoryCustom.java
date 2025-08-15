package com.umc.linkyou.repository.userRepository;

import com.umc.linkyou.domain.Users;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {
    List<Users> findAllByStatusAndInactiveDateBefore(String status, LocalDateTime beforeDateTime);
}
