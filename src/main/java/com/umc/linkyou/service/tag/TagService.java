package com.umc.linkyou.service.tag;

import com.umc.linkyou.web.dto.tag.MyTagRankResponse;

import java.util.List;

public interface TagService {
    List<MyTagRankResponse> getMyTopTags(Long userId, String month, int limit);
}
