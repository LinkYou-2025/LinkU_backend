package com.umc.linkyou.repository.userRepository;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.web.dto.UserResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    UserResponseDTO.UserInfoDTO findUserWithFoldersAndLinks(Long userId);
    List<Users> findAllByStatusAndInactiveDateBefore(String status, LocalDateTime beforeDateTime);
}
