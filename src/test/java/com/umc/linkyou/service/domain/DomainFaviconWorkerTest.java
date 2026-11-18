package com.umc.linkyou.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.DomainSource;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainFaviconWorker 테스트")
class DomainFaviconWorkerTest {

    @InjectMocks private DomainFaviconWorker domainFaviconWorker;

    @Mock private DomainRepository domainRepository;
    @Mock private DomainFaviconService domainFaviconService;

    @Test
    @DisplayName("아이콘이 없는 AUTO 도메인 수집 시 파비콘을 저장한다")
    void 아이콘이_없는_AUTO_도메인_수집_시_파비콘을_저장한다() {
        Domain domain =
                Domain.builder()
                        .domainId(2L)
                        .domainTail("example.com")
                        .name("example")
                        .source(DomainSource.AUTO)
                        .build();
        given(domainRepository.findAutoDomainIdsMissingFavicon()).willReturn(List.of(2L));
        given(domainRepository.findById(2L)).willReturn(Optional.of(domain));
        given(domainFaviconService.collect(domain))
                .willReturn(Optional.of("https://cdn.example/icon.png"));

        domainFaviconWorker.refreshMissingAutoDomainFavicons();

        assertEquals("https://cdn.example/icon.png", domain.getImageUrl());
        verify(domainRepository).save(domain);
    }

    @Test
    @DisplayName("MANUAL 도메인은 파비콘을 자동 수집하지 않는다")
    void MANUAL_도메인은_파비콘을_자동_수집하지_않는다() {
        Domain domain =
                Domain.builder()
                        .domainId(2L)
                        .domainTail("example.com")
                        .name("example")
                        .source(DomainSource.MANUAL)
                        .build();
        given(domainRepository.findAutoDomainIdsMissingFavicon()).willReturn(List.of(2L));
        given(domainRepository.findById(2L)).willReturn(Optional.of(domain));

        domainFaviconWorker.refreshMissingAutoDomainFavicons();

        verify(domainFaviconService, never()).collect(any());
        verify(domainRepository, never()).save(any());
    }
}
