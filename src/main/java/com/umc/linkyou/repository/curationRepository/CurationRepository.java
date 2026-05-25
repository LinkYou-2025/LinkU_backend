package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CurationRepository extends JpaRepository<Curation, Long> {

    boolean existsByUserAndMonth(Users user, String month);

    @Query("""
            SELECT c FROM Curation c
            WHERE c.user.id = :userId
            ORDER BY c.month DESC
            LIMIT 1
            """)
    Optional<Curation> findLatestByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Curation c
            WHERE c.user.id = :userId AND c.month LIKE CONCAT(:year, '-%')
            """)
    List<Curation> findAllByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") String year);
}
