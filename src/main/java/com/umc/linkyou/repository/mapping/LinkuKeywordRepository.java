package com.umc.linkyou.repository.mapping;

import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkuKeywordRepository extends JpaRepository<LinkuKeyword, Long> {
    boolean existsByLinkuAndKeyword(Linku linku, Keyword keyword);
}
