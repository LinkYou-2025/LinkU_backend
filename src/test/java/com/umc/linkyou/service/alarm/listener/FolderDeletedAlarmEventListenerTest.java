package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderDeletedAlarmEventListener 단위 테스트")
class FolderDeletedAlarmEventListenerTest {
    @InjectMocks private FolderDeletedAlarmEventListener listener;

    @Mock private AlarmService alarmService;

    private static final Long MEMBER_A = 2L;
    private static final Long MEMBER_B = 3L;
    private static final Long MEMBER_C = 4L;

    @Nested
    @DisplayName("handle")
    class Handle {
        @Test
        @DisplayName("멤버ids/타입/targetId/payload로 sendAlarmBulk를 한 번 호출한다")
        void bulk_한번호출() {
            List<Long> members = List.of(MEMBER_A, MEMBER_B, MEMBER_C);
            FolderDeletedAlarmEvent event = new FolderDeletedAlarmEvent(
                    FOLDER_ID, members, owner().getNickName(), folder().getFolderName());

            listener.handle(event);

            verify(alarmService).sendAlarmBulk(
                    members,
                    AlarmType.FOLDER_DELETED,
                    FOLDER_ID,
                    new AlarmPayload.NicknameAndFolder(owner().getNickName(), folder().getFolderName()));
        }

        @Test
        @DisplayName("sendAlarmBulk 예외는 async uncaught 핸들러로 전파된다 (리스너에서 삼키지 않음)")
        void 예외_전파() {
            doThrow(new RuntimeException("boom"))
                    .when(alarmService).sendAlarmBulk(any(), any(), any(), any());

            FolderDeletedAlarmEvent event = new FolderDeletedAlarmEvent(
                    FOLDER_ID, List.of(MEMBER_A), owner().getNickName(), folder().getFolderName());

            assertThatThrownBy(() -> listener.handle(event))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
