package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interests, Long> {
    void deleteAllByUser(Users user);

    @Query("select i.interest from Interests i where i.user.id = :userId")
    List<String> findAllInterestNamesByUserId(Long userId);
}
