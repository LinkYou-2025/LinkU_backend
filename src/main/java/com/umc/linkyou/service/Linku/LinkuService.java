package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LinkuService {
    ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(Long userId, String url);

    ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailGetLinku(Long userId, Long linkuId);

    List<LinkuResponseDTO.LinkuSimpleDTO> getRecentViewedLinkus(Long userId, int limit);

    LinkuResponseDTO.LinkuResultDTO updateLinku(Long userId, Long linkuId, LinkuRequestDTO.LinkuUpdateDTO updateDTO);


    void deleteUsersLinku(Long userId, Long userLinkuId);
}
