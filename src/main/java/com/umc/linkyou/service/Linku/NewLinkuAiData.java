package com.umc.linkyou.service.Linku;

record NewLinkuAiData(
        Long aiCategoryId,
        String keywords,
        Long aiEmotionId,
        Long aiSituationId,
        String aiTitle,
        String crawledImgUrl) {}
