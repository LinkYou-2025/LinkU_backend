package com.umc.linkyou.service.Linku;

import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface LinkuCreateService {
    LinkuResponseDTO.LinkuCreateResult createLinku(Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image);

}
