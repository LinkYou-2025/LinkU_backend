package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.UsersPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Users - Purposes 다대다 조인 레포지토리
public interface UsersPurposeRepository extends JpaRepository<UsersPurpose, Long> {
    void deleteAllByUser(Users user);

    @Query("select up.purpose.name from UsersPurpose up where up.user.id = :userId")
    List<String> findAllPurposeNamesByUserId(@Param("userId") Long userId);
}
