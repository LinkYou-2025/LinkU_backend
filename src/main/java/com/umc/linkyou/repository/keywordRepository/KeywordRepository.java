package com.umc.linkyou.repository.keywordRepository;

import com.umc.linkyou.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByName(String name);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO keywords (name, created_at, updated_at)
        VALUES (:name, now(), now())
        ON CONFLICT (name) DO NOTHING
        """, nativeQuery = true)
    int insertIgnore(@Param("name") String name);
}
