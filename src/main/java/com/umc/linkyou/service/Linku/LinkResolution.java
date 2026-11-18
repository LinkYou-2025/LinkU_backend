package com.umc.linkyou.service.Linku;

import java.util.Optional;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.service.Linku.LinkuMetrics.ResolveOutcome;

// outcome은 저장 API 응답 시간을 경로별로 나눠 기록하는 데 쓴다.
record LinkResolution(
        boolean validUrl,
        String linkUrl,
        Optional<Linku> existing,
        NewLinkuAiData aiData,
        PageType pageType,
        ResolveOutcome outcome) {
    LinkResolution(
            boolean validUrl,
            String linkUrl,
            Optional<Linku> existing,
            NewLinkuAiData aiData,
            PageType pageType) {
        this(validUrl, linkUrl, existing, aiData, pageType, null);
    }

    LinkResolution(
            boolean validUrl, String linkUrl, Optional<Linku> existing, NewLinkuAiData aiData) {
        this(validUrl, linkUrl, existing, aiData, null, null);
    }

    LinkResolution withOutcome(ResolveOutcome outcome) {
        return new LinkResolution(validUrl, linkUrl, existing, aiData, pageType, outcome);
    }
}
