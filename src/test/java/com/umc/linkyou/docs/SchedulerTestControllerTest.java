package com.umc.linkyou.docs;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/docs")
@Profile("test")  // RestDocs용 더미
public class SchedulerTestControllerTest {

    @GetMapping("/scheduler/test")
    public Map<String, Object> testDeleteInactiveUsers(
            @RequestParam(defaultValue = "10") int daysAgo) {

        return Map.of(
                "message", "RestDocs 테스트용",
                "daysThreshold", daysAgo,
                "deletedCount", 0
        );
    }
}
