package com.umc.linkyou.docs;

import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/docs")
@Profile("!prod")
public class SchedulerTestController {  // 🔥 RequiredArgsConstructor 제거

    @Autowired  // 🔥 직접 주입
    private UserService userService;

    @GetMapping("/scheduler/test")
    public Map<String, Object> testDeleteInactiveUsers(
            @RequestParam(defaultValue = "10") int daysAgo) {

        ((UserServiceImpl) userService).deleteCompletelyInactiveUsers();
        return Map.of(
                "message", "스케줄러 테스트 완료",
                "daysThreshold", daysAgo,
                "deletedCount", 3,
                "sampleUserIds", List.of(1L, 2L, 3L)
        );
    }

    @PostMapping("/scheduler/manual")
    public Map<String, Object> manualSchedulerRun() {
        ((UserServiceImpl) userService).deleteCompletelyInactiveUsers();
        return Map.of("status", "스케줄러 수동 실행 완료");
    }
}
