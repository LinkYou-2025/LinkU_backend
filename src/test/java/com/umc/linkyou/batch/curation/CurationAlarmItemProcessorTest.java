package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.repository.AlarmSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurationAlarmItemProcessor 단위 테스트")
class CurationAlarmItemProcessorTest {

    @InjectMocks private CurationAlarmItemProcessor processor;

    @Mock private AlarmSettingRepository alarmSettingRepository;

    private static final Long USER_ID = 1L;
    private static final Long CURATION_ID = 100L;

    private Users user() {
        return Users.builder().id(USER_ID).nickName("테스트유저").role(Role.USER).build();
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("큐레이션 알림 설정이 켜져 있으면 배치 아이템을 반환한다")
        void 설정켜짐_배치아이템_반환() {
            AlarmSetting setting = AlarmSetting.createDefault(user());
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

            CurationAlarmBatchItem result = processor.process(
                    new CurationAlarmCandidate(CURATION_ID, USER_ID, "테스트유저"));

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.curationId()).isEqualTo(CURATION_ID);
            assertThat(result.nickname()).isEqualTo("테스트유저");
        }
    }

    @Nested
    @DisplayName("스킵")
    class Skip {

        @Test
        @DisplayName("큐레이션 알림 설정이 꺼져 있으면 null을 반환한다")
        void 설정꺼짐_null_반환() {
            AlarmSetting setting = AlarmSetting.createDefault(user());
            setting.updateCuration(false);
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.of(setting));

            CurationAlarmBatchItem result = processor.process(
                    new CurationAlarmCandidate(CURATION_ID, USER_ID, "테스트유저"));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("알림 설정이 초기화되지 않은 유저면 null을 반환한다")
        void 설정미초기화_null_반환() {
            given(alarmSettingRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            CurationAlarmBatchItem result = processor.process(
                    new CurationAlarmCandidate(CURATION_ID, USER_ID, "테스트유저"));

            assertThat(result).isNull();
        }
    }
}
