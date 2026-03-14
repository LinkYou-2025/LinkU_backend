package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findById(long l);
    boolean existsByCategoryName(String categoryName);
}
