package com.umc.linkyou.repository.aiArticleRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;

public interface AiArticleRepository
        extends JpaRepository<AiArticle, Long>, AiArticleRepositoryCustom {
    Optional<AiArticle> findByLinku(Linku linku);
}
