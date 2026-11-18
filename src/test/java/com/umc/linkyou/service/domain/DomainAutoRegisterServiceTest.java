package com.umc.linkyou.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.DomainSource;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainAutoRegisterService 테스트")
class DomainAutoRegisterServiceTest {

    @InjectMocks private DomainAutoRegisterService domainAutoRegisterService;

    @Mock private DomainRepository domainRepository;

    @Nested
    @DisplayName("기존 행 재사용")
    class ExistingDomain {

        @Test
        @DisplayName("하위 도메인 행이 없어도 apex 행이 있으면 그 행을 쓴다")
        void apex_행이_있으면_그_행을_쓴다() {
            Domain tistory =
                    Domain.builder().domainId(8L).name("tistory").domainTail("tistory.com").build();
            given(domainRepository.findByDomainTail("someuser.tistory.com"))
                    .willReturn(Optional.empty());
            given(domainRepository.findByDomainTail("tistory.com"))
                    .willReturn(Optional.of(tistory));

            assertEquals(
                    Optional.of(tistory),
                    domainAutoRegisterService.resolveOrRegister(
                            "https://someuser.tistory.com/123"));
            verify(domainRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("자동 등록")
    class Register {

        @Test
        @DisplayName("처음 보는 사이트는 네트워크 요청 없이 최소 AUTO 행을 만든다")
        void 처음_보는_사이트는_최소_AUTO_행을_만든다() {
            given(domainRepository.findByDomainTail(any())).willReturn(Optional.empty());
            given(domainRepository.save(any(Domain.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            domainAutoRegisterService.resolveOrRegister("https://news.example.com/article/1");

            ArgumentCaptor<Domain> captor = ArgumentCaptor.forClass(Domain.class);
            verify(domainRepository).save(captor.capture());
            assertEquals("example.com", captor.getValue().getDomainTail());
            assertEquals("example", captor.getValue().getName());
            assertEquals(DomainSource.AUTO, captor.getValue().getSource());
            assertNull(captor.getValue().getImageUrl());
        }

        @Test
        @DisplayName("동시에 같은 도메인이 등록되면 먼저 저장된 행을 재사용한다")
        void 동시에_같은_도메인이_등록되면_먼저_저장된_행을_재사용한다() {
            Domain existing =
                    Domain.builder().domainId(7L).name("example").domainTail("example.com").build();
            given(domainRepository.findByDomainTail("news.example.com"))
                    .willReturn(Optional.empty());
            given(domainRepository.findByDomainTail("example.com"))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(existing));
            given(domainRepository.save(any(Domain.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate domain_tail"));

            assertEquals(
                    Optional.of(existing),
                    domainAutoRegisterService.resolveOrRegister(
                            "https://news.example.com/article/1"));
        }
    }
}
