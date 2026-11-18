package com.umc.linkyou.service.Linku;

import static com.umc.linkyou.service.Linku.LinkuMetrics.timed;
import static com.umc.linkyou.service.Linku.LinkuMetrics.timedRun;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.service.Linku.LinkuMetrics.Stage;
import com.umc.linkyou.service.domain.DomainAutoRegisterService;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkuCreateService {
    private final LinkResolver linkResolver;
    private final LinkuClassificationResolver classificationResolver;
    private final DomainAutoRegisterService domainAutoRegisterService;
    private final AwsS3Service awsS3Service;
    private final LinkuPersister linkuPersister;
    private final LinkuMetrics linkuMetrics;

    // 외부 I/O는 트랜잭션 밖에서 하고 DB 처리만 짧은 트랜잭션으로 감싼다.
    // 재사용 경로와 새 링크 경로의 응답 시간이 크게 달라 outcome별로 기록한다.
    public LinkuResponseDTO.LinkuCreateResult createLinku(
            Long userId, LinkuRequestDTO.LinkuCreateDTO dto, MultipartFile image) {
        long startNanos = System.nanoTime();
        LinkuMetrics.ResolveOutcome outcome = null;
        boolean success = false;
        try {
            String inputUrl = validateAndNormalizeUrl(dto.getLinku());
            LinkResolution resolution = linkResolver.resolve(inputUrl);
            outcome = resolution.outcome();
            LinkuResponseDTO.LinkuCreateResult result =
                    save(userId, dto, image, inputUrl, resolution);
            success = true;
            return result;
        } finally {
            linkuMetrics.recordCreate(outcome, success, System.nanoTime() - startNanos);
        }
    }

    private LinkuResponseDTO.LinkuCreateResult save(
            Long userId,
            LinkuRequestDTO.LinkuCreateDTO dto,
            MultipartFile image,
            String inputUrl,
            LinkResolution resolution) {
        String linkUrl = resolution.linkUrl();
        String domainTail = UrlValidUtils.extractDomainTail(linkUrl);
        List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(linkUrl);
        timedRun(
                linkuMetrics,
                Stage.VALIDATE,
                () ->
                        classificationResolver.validateBeforeUpload(
                                userId, dto, resolution.existing(), resolution.aiData()));
        // 도메인 자동 등록도 트랜잭션 밖에서 미리 한다.
        Domain preResolvedDomain =
                resolution.existing().isEmpty()
                        ? timed(
                                        linkuMetrics,
                                        Stage.DOMAIN_REGISTER,
                                        () -> domainAutoRegisterService.resolveOrRegister(linkUrl))
                                .orElse(null)
                        : null;
        String userImageUrl = uploadUserImage(image);
        PersistCommand command =
                new PersistCommand(
                        userId,
                        dto,
                        inputUrl,
                        linkUrl,
                        domainTail,
                        domainTailCandidates,
                        preResolvedDomain,
                        resolution.aiData(),
                        resolution.pageType(),
                        userImageUrl);
        LinkuResponseDTO.LinkuResultDTO result =
                timed(linkuMetrics, Stage.PERSIST, () -> linkuPersister.persist(command));
        return LinkuResponseDTO.LinkuCreateResult.builder()
                .data(result)
                .validUrl(resolution.validUrl())
                .build();
    }

    public String validateAndNormalizeUrl(String url) {
        return UrlValidUtils.normalizeAndValidateLinkuUrl(url);
    }

    private String uploadUserImage(MultipartFile image) {
        return image != null && !image.isEmpty()
                ? timed(
                        linkuMetrics,
                        Stage.S3_UPLOAD,
                        () -> awsS3Service.uploadFile(image, "linkucreate"))
                : null;
    }
}
