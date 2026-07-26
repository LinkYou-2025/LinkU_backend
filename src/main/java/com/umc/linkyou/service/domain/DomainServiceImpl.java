package com.umc.linkyou.service.domain;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.converter.DomainConverter;
import com.umc.linkyou.domain.Image;
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
        Image domainImage = null;
        if (image != null && !image.isEmpty()) {
            String imageKey = awsS3Service.uploadFile(image, "domain");
            domainImage = Image.ofS3(imageKey);
        }

        Domain domain = Domain.builder()
                .name(dto.getName())
                .domainTail(dto.getDomainTail())
                .image(domainImage)
                .build();
        domain = domainRepository.save(domain);
        return DomainConverter.toDomainResponseDTO(domain.getName(), domain.getDomainTail(), awsS3Service.resolveUrl(domain.getImage()));
    }// 도메인 생성

    @Override
    @Transactional
    public DomainDTO.DomainReponseDTO updateDomain(Long userId, DomainDTO.DomainRequestDTO dto, MultipartFile image) {
        Domain domain = domainRepository.findById(dto.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND));
        // null 아닌 필드만 업데이트
        if (dto.getName() != null) {
            domain.updateName(dto.getName());
        }
        if (dto.getDomainTail() != null) {
            domain.updateDomainTail(dto.getDomainTail());
        }
        if (image != null && !image.isEmpty()) {
            // 기존 이미지가 있으면 S3에서 먼저 삭제한 뒤 새 이미지를 업로드하고 key를 교체한다.
            String oldKey = domain.getImage() != null ? domain.getImage().getLocation() : null;
            String newKey = awsS3Service.replaceFile(oldKey, image, "domain");
            if (domain.getImage() != null) {
                domain.getImage().updateLocation(newKey);
            } else {
                domain.updateImage(Image.ofS3(newKey));
            }
        }

        domainRepository.save(domain);

        return DomainDTO.DomainReponseDTO.builder()
                .name(domain.getName())
                .domainTail(domain.getDomainTail())
                .imageUrl(awsS3Service.resolveUrl(domain.getImage()))
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
                        .imageUrl(awsS3Service.resolveUrl(d.getImage()))
                        .build())
                .toList();

        return DomainDTO.DomainCursorPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(domains.size() == size) // 더 가져올 데이터가 있는 경우
                .build();
    }

}
