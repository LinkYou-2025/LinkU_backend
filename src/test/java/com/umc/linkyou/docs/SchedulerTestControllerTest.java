package com.umc.linkyou.docs;

import com.umc.linkyou.service.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/docs")
@Profile("test")
public class SchedulerTestControllerTest {  // ✅ 클래스명 정상

    @Autowired  // 🔥 직접 주입
    private UserService userService;  // ✅ 초기화됨

    @GetMapping("/scheduler/test")
    public Map<String, Object> testDeleteInactiveUsers(
            @RequestParam(defaultValue = "10") int daysAgo) {

        // 🔥 리플렉션 제거 → 더미 데이터로 테스트용
        // 실제 스케줄러는 별도 통합테스트에서 검증

        return Map.of(
                "message", "스케줄러 테스트 완료 (RestDocs용)",
                "daysThreshold", daysAgo,
                "deletedCount", 3,  // 테스트 데이터 기준
                "sampleUserIds", List.of(1L, 2L, 3L)
        );
    }

    @PostMapping("/scheduler/manual")
    public Map<String, Object> manualSchedulerRun() {
        // 실제 호출 대신 상태 반환
        return Map.of(
                "status", "스케줄러 수동 실행 완료 (RestDocs용)",
                "executedAt", java.time.LocalDateTime.now()
        );
    }
}
