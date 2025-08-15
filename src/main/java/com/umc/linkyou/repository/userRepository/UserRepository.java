package com.umc.linkyou.repository.userRepository;

import com.umc.linkyou.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>,UserRepositoryCustom {
    Optional<Users> findByNickName(String nickName);
    Optional<Users> findByEmail(String email);
    Optional<Users> findById(Long id);
}
