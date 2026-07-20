package com.umc.linkyou.repository.keywordRepository;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// upsertCount()가 실제 PostgreSQL(Testcontainers)에서 ON CONFLICT 문법으로
// 정상 동작하는지 검증한다. #369에서 MySQL 전용 ON DUPLICATE KEY UPDATE 문법과
// 잘못된 테이블명(keyword_monthly_count)으로 인해 COMMON500이 발생했던 회귀 방지 목적.
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@DisplayName("KeywordMonthlyCountRepository 테스트")
class KeywordMonthlyCountRepositoryTest {

    @Autowired private KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    @Autowired private UserRepository userRepository;

    @Nested
    @DisplayName("upsertCount")
    class UpsertCount {

        @Test
        @DisplayName("최초 호출 시 count가 1로 저장된다")
        void 최초_호출_시_count가_1로_저장된다() {
            // given
            Users user = userRepository.save(createUser("keyword-user-1"));

            // when
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.EMOTION.name(), 1L, "2026-07");

            // then
            List<KeywordMonthlyCount> result = keywordMonthlyCountRepository
                    .findTopByUserIdAndBaseMonthAndType(user.getId(), "2026-07", KeywordType.EMOTION, PageRequest.of(0, 10));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 user_id, type, ref_id, base_month로 반복 호출하면 count가 누적된다")
        void 같은_키로_반복_호출하면_count가_누적된다() {
            // given
            Users user = userRepository.save(createUser("keyword-user-2"));

            // when
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.SITUATION.name(), 18L, "2026-07");
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.SITUATION.name(), 18L, "2026-07");
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.SITUATION.name(), 18L, "2026-07");

            // then
            List<KeywordMonthlyCount> result = keywordMonthlyCountRepository
                    .findTopByUserIdAndBaseMonthAndType(user.getId(), "2026-07", KeywordType.SITUATION, PageRequest.of(0, 10));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("ref_id가 다르면 별도의 행으로 저장된다")
        void refId가_다르면_별도의_행으로_저장된다() {
            // given
            Users user = userRepository.save(createUser("keyword-user-3"));

            // when
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.EMOTION.name(), 1L, "2026-07");
            keywordMonthlyCountRepository.upsertCount(user.getId(), KeywordType.EMOTION.name(), 2L, "2026-07");

            // then
            List<KeywordMonthlyCount> result = keywordMonthlyCountRepository
                    .findTopByUserIdAndBaseMonthAndType(user.getId(), "2026-07", KeywordType.EMOTION, PageRequest.of(0, 10));
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(k -> k.getCount() == 1);
        }
    }

    private Users createUser(String nickName) {
        return Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build();
    }
}
