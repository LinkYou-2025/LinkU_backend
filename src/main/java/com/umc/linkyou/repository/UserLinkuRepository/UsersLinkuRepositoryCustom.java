package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.mapping.UsersLinku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface UsersLinkuRepositoryCustom {
    // 특정 유저의 링크들을 카테고리별로 조회 (AiArticle 정보 포함)
    List<UsersLinku> fetchAiArticlesByCategoryId(Long userId, Long categoryId);
    List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit);
    List<UsersLinku> fetchAiArticlesByCategoryIdWithCursor(Long userId, Long categoryId, Long cursorId, int limit);
}
