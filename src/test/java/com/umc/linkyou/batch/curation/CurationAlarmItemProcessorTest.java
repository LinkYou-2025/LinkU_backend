package com.umc.linkyou.batch.curation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurationAlarmItemProcessor 단위 테스트")
class CurationAlarmItemProcessorTest {

    private final CurationAlarmItemProcessor processor = new CurationAlarmItemProcessor();

    private static final Long USER_ID = 1L;
    private static final Long CURATION_ID = 100L;

    @Test
    @DisplayName("candidate를 batch item으로 그대로 변환한다")
    void candidate를_batch_item으로_변환한다() {
        CurationAlarmBatchItem result = processor.process(
                new CurationAlarmCandidate(CURATION_ID, USER_ID, "테스트유저"));

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.curationId()).isEqualTo(CURATION_ID);
        assertThat(result.nickname()).isEqualTo("테스트유저");
    }
}
