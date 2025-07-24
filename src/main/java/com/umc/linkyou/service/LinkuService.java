package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LinkuService {
    LinkuResponseDTO.LinkuResultDTO createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) ;

    ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuIsExistDTO>> existLinku(Long userId, String url);

    ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuResultDTO>> detailGetLinku(Long userId, Long linkuId);

    List<LinkuResponseDTO.LinkuSimpleDTO> getRecentViewedLinkus(Long userId, int limit);

    LinkuResponseDTO.LinkuResultDTO updateLinku(Long userId, Long linkuId, LinkuRequestDTO.LinkuUpdateDTO updateDTO);

    ResponseEntity<ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size);
}
