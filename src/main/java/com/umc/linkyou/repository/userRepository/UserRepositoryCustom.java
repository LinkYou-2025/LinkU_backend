package com.umc.linkyou.repository.userRepository;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.web.dto.UserResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
//    UserResponseDTO.UserInfoDTO findUserWithFoldersAndLinks(Long userId);
//    List<Users> findAllByStatusAndInactiveDateBefore(String status, LocalDateTime beforeDateTime);
    List<Long> findInactiveUserIds(LocalDateTime beforeDateTime);
    Optional<Users> findByEmailAndStatus(String email, UserStatus status);
    List<String> findAllPurposeNamesByUserId(Long userId);
    List<String> findAllInterestNamesByUserId(Long userId);
}
