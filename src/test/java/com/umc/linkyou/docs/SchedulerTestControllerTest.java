package com.umc.linkyou.docs;

import com.umc.linkyou.service.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/docs")
@RequiredArgsConstructor
public class SchedulerTestControllerTest {

    private final UserService userService;

    /**
     * 🔥 RestDocs 문서화용 테스트 엔드포인트
     * 실제 운영에서는 제거하거나 @Profile("test") 사용
     */
    @GetMapping("/scheduler/test")
    public Map<String, Object> testDeleteInactiveUsers(
            @RequestParam(defaultValue = "10") int daysAgo) {

        // 스케줄러 수동 실행
        userService.deleteCompletelyInactiveUsers();

        return Map.of(
                "message", "스케줄러 테스트 완료",
                "daysThreshold", daysAgo,
                "deletedCount", 3,  // 테스트 데이터 기준
                "sampleUserIds", List.of(1L, 2L, 3L)
        );
    }

    @PostMapping("/scheduler/manual")
    public Map<String, Object> manualSchedulerRun() {
        userService.deleteCompletelyInactiveUsers();
        return Map.of("status", "스케줄러 수동 실행 완료");
    }
}
