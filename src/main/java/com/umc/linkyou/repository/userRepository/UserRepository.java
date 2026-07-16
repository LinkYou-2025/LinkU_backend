package com.umc.linkyou.repository.userRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.umc.linkyou.domain.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>, UserRepositoryCustom {
    Optional<Users> findByNickName(String nickName);

    Optional<Users> findById(Long id);

    boolean existsByNickName(String nickname);

    @Query("SELECT u.id FROM Users u")
    List<Long> findAllIds(Pageable pageable);

    @Query("SELECT u.nickName FROM Users u WHERE u.id = :userId")
    Optional<String> findNickNameById(@Param("userId") Long userId);
}
