package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Purposes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurposeRepository extends JpaRepository<Purposes, Long> {
    void deleteAllByUser(Users user);

    @Query("select p.purpose from Purposes p where p.user.id = :userId")
    List<String> findAllPurposeNamesByUserId(Long userId);
}
