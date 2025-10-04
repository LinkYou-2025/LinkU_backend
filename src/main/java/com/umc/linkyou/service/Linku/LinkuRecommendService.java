package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

import java.util.List;

public interface LinkuRecommendService {
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size);

}
