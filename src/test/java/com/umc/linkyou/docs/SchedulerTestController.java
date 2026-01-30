package com.umc.linkyou.docs;

import com.umc.linkyou.service.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/scheduler")
@Profile({"!prod", "!test"})
public class SchedulerTestController {

    @Autowired
    private UserService userService;

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> testDeleteInactive(
            @RequestParam Long userId) {

        userService.testImmediateDelete(userId);
        return Map.of(
                "message", "사용자 삭제 완료",
                "deletedUserId", userId
        );
    }
}
