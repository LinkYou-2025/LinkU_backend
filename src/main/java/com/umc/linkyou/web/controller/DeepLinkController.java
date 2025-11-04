package com.umc.linkyou.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "deeplink-controller", description = "딥링크 리디렉션 페이지")
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class DeepLinkController {
    
    @Value("${app.deeplink.base-url}")
    private String serverBaseUrl;
    
    @GetMapping("/open")
    public String openPage(@RequestParam(value = "action", required = false) String action,
                           @RequestParam(value = "folderId", required = false) String folderId,
                           Model model) {
        model.addAttribute("action", action);
        model.addAttribute("folderId", folderId);
        model.addAttribute("serverBaseUrl", serverBaseUrl);
        return "open"; // Thymeleaf 등 템플릿 엔진으로 open.html 렌더링
    }

}
