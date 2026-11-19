package com.umc.linkyou.service.tag;

import java.time.YearMonth;
import java.util.List;

import com.umc.linkyou.web.dto.tag.MyTagRankResponse;

public interface TagService {
    List<MyTagRankResponse> getMyTopTags(Long userId, YearMonth month, int limit);
}
