package com.umc.linkyou.repository.UserLinkuRepository;

import static org.junit.jupiter.api.Assertions.*;

import com.umc.linkyou.repository.dto.LinkuKeywordRow;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.support.fixture.LinkuDetailFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@Transactional
@DisplayName("링크 상세조회 쿼리 통합 테스트")
class LinkuDetailRepositoryTest {
    @Autowired private EntityManager em;
    @Autowired private UsersLinkuRepository usersLinkuRepository;
    @Autowired private LinkuFolderRepository linkuFolderRepository;
    @Autowired private LinkuKeywordRepository linkuKeywordRepository;

    @Nested
    @DisplayName("성공")
    class Success {
        @Test
        @DisplayName("AI_상세_조회_시_연관데이터의_추가_SELECT없이_결과를_반환한다")
        void AI_상세_조회_시_연관데이터의_추가_SELECT없이_결과를_반환한다() {
            var saved = LinkuDetailFixture.persist(em, true, true);
            var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
            stats.clear();
            var ul = usersLinkuRepository.findDetailByUserIdAndUserLinkuId(saved.userId(), saved.userLinkuId())
                    .orElseThrow();
            var linku = ul.getLinku();
            assertEquals("개인 제목", ul.getTitle());
            assertEquals("기대", ul.getEmotion().getName());
            assertEquals("공부", ul.getSituation().getName());
            assertEquals("기술", linku.getCategory().getCategoryName());
            assertEquals("example", linku.getDomain().getName());
            assertEquals("테스트 요약", linku.getAiArticle().getSummary());
            assertEquals(1L, stats.getPrepareStatementCount());

            var folder = linkuFolderRepository
                    .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(saved.userLinkuId()).orElseThrow();
            assertEquals("최신 폴더", folder.getFolder().getFolderName());
            assertEquals(2L, stats.getPrepareStatementCount());

            var names = linkuKeywordRepository.findKeywordNamesByLinkuIdIn(List.of(saved.linkuId()))
                    .stream().map(LinkuKeywordRow::name).sorted().toList();
            assertEquals(List.of("Java", "Spring"), names);
            assertEquals(3L, stats.getPrepareStatementCount());
        }

        @Test
        @DisplayName("상황과_AI요약이_없을_시_링크를_누락하지_않고_반환한다")
        void 상황과_AI요약이_없을_시_링크를_누락하지_않고_반환한다() {
            var saved = LinkuDetailFixture.persist(em, false, false);
            var ul = usersLinkuRepository.findDetailByUserIdAndUserLinkuId(saved.userId(), saved.userLinkuId())
                    .orElseThrow();
            assertEquals(saved.userLinkuId(), ul.getUserLinkuId());
            assertEquals("원문 제목", ul.getLinku().getTitle());
            assertNull(ul.getSituation());
            assertNull(ul.getLinku().getAiArticle());
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {
        @Test
        @DisplayName("타인_링크_조회_시_결과를_반환하지_않는다")
        void 타인_링크_조회_시_결과를_반환하지_않는다() {
            var saved = LinkuDetailFixture.persist(em, false, false);
            assertEquals(java.util.Optional.empty(), usersLinkuRepository.findDetailByUserIdAndUserLinkuId(
                    saved.userId() + 1L, saved.userLinkuId()));
        }
    }
}
