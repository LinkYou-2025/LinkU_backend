package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.UsersInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Users - Interests 다대다 조인 레포지토리
public interface UsersInterestRepository extends JpaRepository<UsersInterest, Long> {
    void deleteAllByUser(Users user);

    @Query("select ui.interest.name from UsersInterest ui where ui.user.id = :userId")
    List<String> findAllInterestNamesByUserId(@Param("userId") Long userId);
}
