package com.umc.linkyou.service.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.DomainSource;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.utils.UrlValidUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 링크 저장이 외부 사이트 응답에 묶이지 않게 이름은 도메인 라벨로만 채운다.
// 파비콘은 DomainFaviconWorker가 아이콘 없는 AUTO 도메인을 모아 수집한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainAutoRegisterService {

    private final DomainRepository domainRepository;

    public Optional<Domain> resolveOrRegister(String url) {
        List<String> candidates = UrlValidUtils.extractDomainTailCandidates(url);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Optional<Domain> existing =
                candidates.stream()
                        .map(domainRepository::findByDomainTail)
                        .flatMap(Optional::stream)
                        .findFirst();
        if (existing.isPresent()) {
            return existing;
        }

        // 마지막 후보가 등록 도메인이라 그 단위로 등록한다.
        String apex = candidates.get(candidates.size() - 1);
        try {
            Domain domain =
                    domainRepository.save(
                            Domain.builder()
                                    .name(labelOf(apex))
                                    .domainTail(apex)
                                    .source(DomainSource.AUTO)
                                    .build());
            log.info("[도메인 자동 등록] {} (파비콘은 일일 크론이 수집)", apex);
            return Optional.of(domain);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 같은 apex를 먼저 만들었다면 그 행을 쓴다.
            return domainRepository.findByDomainTail(apex);
        } catch (Exception e) {
            log.warn("[도메인 자동 등록 실패] {}: {}", apex, e.getMessage());
            return Optional.empty();
        }
    }

    static String labelOf(String apex) {
        int dot = apex.indexOf('.');
        return dot > 0 ? apex.substring(0, dot) : apex;
    }
}
