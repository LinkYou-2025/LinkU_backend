package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Situation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SituationRepository extends JpaRepository<Situation, Long> {

    @Query("SELECT s.name FROM Situation s WHERE s.id = :id")
    Optional<String> findNameById(@Param("id") Long id);
}
