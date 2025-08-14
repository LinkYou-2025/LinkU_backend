package com.umc.linkyou.service.domain;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awsS3.AwsS3Service;
import com.umc.linkyou.converter.DomainConverter;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.web.dto.DomainDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService{
    private final DomainRepository domainRepository;
    private final AwsS3Service awsS3Service;

    @Override
    @Transactional
    public DomainDTO.DomainReponseDTO createDomain(Long userId, DomainDTO.DomainRequestDTO dto, MultipartFile image) {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = awsS3Service.uploadFile(image, "domain");
        }

        Domain domain = Domain.builder()
                .name(dto.getName())
                .domainTail(dto.getDomainTail())
                .imageUrl(imageUrl)
                .build();
        domain = domainRepository.save(domain);
        return DomainConverter.toDomainResponseDTO(domain.getName(), domain.getDomainTail(), domain.getImageUrl());
    }// 도메인 생성

    @Override
    @Transactional
    public DomainDTO.DomainReponseDTO updateDomain(Long userId, DomainDTO.DomainRequestDTO dto, MultipartFile image) {
        Domain domain = domainRepository.findById(dto.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
        // null 아닌 필드만 업데이트
        if (dto.getName() != null) {
            domain.setName(dto.getName());
        }
        if (dto.getDomainTail() != null) {
            domain.setDomainTail(dto.getDomainTail());
        }
        if (image != null && !image.isEmpty()) {
            // 기존 이미지가 있을 경우 S3에서 삭제
            if (domain.getImageUrl() != null) {
                awsS3Service.deleteFileByUrl(domain.getImageUrl());  // URL에서 파일명 추출 후 삭제 실행
            }
            // 새 이미지 업로드 후 URL 세팅
            String imageUrl = awsS3Service.uploadFile(image, "domain");
            domain.setImageUrl(imageUrl);
        }

        domainRepository.save(domain);

        return DomainDTO.DomainReponseDTO.builder()
                .name(domain.getName())
                .domainTail(domain.getDomainTail())
                .imageUrl(domain.getImageUrl())
                .build();
    }
//도메인 수정

    @Override
    @Transactional
    public DomainDTO.DomainCursorPageResponse getDomainsCursor(Long lastDomainId, int size) {
        List<Domain> domains = domainRepository.findDomainsCursorPaging(lastDomainId, size);

        // nextCursor 셋팅: 마지막 원소의 ID
        Long nextCursor = domains.isEmpty() ? null : domains.get(domains.size() - 1).getDomainId();

        List<DomainDTO.DomainReponseDTO> items = domains.stream()
                .map(d -> DomainDTO.DomainReponseDTO.builder()
                        .name(d.getName())
                        .domainTail(d.getDomainTail())
                        .imageUrl(d.getImageUrl())
                        .build())
                .toList();

        return DomainDTO.DomainCursorPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(domains.size() == size) // 더 가져올 데이터가 있는 경우
                .build();
    }

}
