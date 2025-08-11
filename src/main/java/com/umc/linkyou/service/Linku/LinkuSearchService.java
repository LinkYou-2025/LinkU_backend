package com.umc.linkyou.service.Linku;

import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import java.util.List;

public interface LinkuSearchService {
    List<LinkuSearchSuggestionResponse> suggest(Long userId, String keyword);
}