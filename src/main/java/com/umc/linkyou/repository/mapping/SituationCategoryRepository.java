package com.umc.linkyou.repository.mapping;

import com.umc.linkyou.domain.mapping.SituationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SituationCategoryRepository extends JpaRepository<SituationCategory, Long> {
    List<SituationCategory> findBySituation_Id(Long situationId);

    @Query("SELECT sc.category.categoryId FROM SituationCategory sc WHERE sc.situation.id = :situationId")
    List<Long> findCategoryIdsBySituationId(@Param("situationId") Long situationId);
}
