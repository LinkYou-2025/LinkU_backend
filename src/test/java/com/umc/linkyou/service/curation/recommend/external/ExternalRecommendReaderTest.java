package com.umc.linkyou.service.curation.recommend.external;

import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Image;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepositoryCustom;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalRecommendReader 테스트")
class ExternalRecommendReaderTest {

    @InjectMocks
    private ExternalRecommendReader externalRecommendReader;

    @Mock private CurationLinkuRepository curationLinkuRepository;
    @Mock private DomainRepositoryCustom domainRepository;
    @Mock private AwsS3Service awsS3Service;

    private static final Long CURATION_ID = 1L;

    private CurationLinku externalItem(String url) {
        Curation curation = Curation.builder().curationId(CURATION_ID).build();
        return CurationLinku.ofExternal(curation, url, "제목", "https://img.example.com/thumb.jpg");
    }

    @Nested
    @DisplayName("도메인 tail 계층 매칭")
    class DomainTailHierarchyMatching {

        @Test
        @DisplayName("정확히 일치하는 도메인이 있으면 그 도메인 정보로 응답을 채운다")
        void 정확히_일치하면_해당_도메인_정보를_채운다() {
            String url = "https://example.com/post";
            given(curationLinkuRepository.findByCurationIdAndType(CURATION_ID, CurationLinkuType.EXTERNAL))
                    .willReturn(List.of(externalItem(url)));

            Domain exact = Domain.builder().name("example").domainTail("example.com").build();
            given(domainRepository.findByDomainTailIn(any())).willReturn(List.of(exact));

            List<RecommendedLinkResponse> result = externalRecommendReader.read(CURATION_ID);

            assertEquals(1, result.size());
            assertEquals("example", result.get(0).getDomain());
        }

        @Test
        @DisplayName("정확히 일치하는 도메인이 없으면 registry-suffix apex(tistory.com) 도메인 정보로 폴백한다")
        void 정확히_일치하지_않으면_apex_도메인으로_폴백한다() {
            String url = "https://someuser.tistory.com/123";
            given(curationLinkuRepository.findByCurationIdAndType(CURATION_ID, CurationLinkuType.EXTERNAL))
                    .willReturn(List.of(externalItem(url)));

            // DB에는 someuser.tistory.com 정확 매칭 행은 없고 apex인 tistory.com만 있는 상황을 재현.
            Domain apex = Domain.builder().name("티스토리").domainTail("tistory.com").image(Image.ofS3("tistory-icon.png")).build();
            given(domainRepository.findByDomainTailIn(any())).willReturn(List.of(apex));
            given(awsS3Service.resolveUrl(apex.getImage())).willReturn("tistory-icon.png");

            List<RecommendedLinkResponse> result = externalRecommendReader.read(CURATION_ID);

            assertEquals(1, result.size());
            assertEquals("티스토리", result.get(0).getDomain());
            assertEquals("tistory-icon.png", result.get(0).getDomainImageUrl());
        }

        @Test
        @DisplayName("배치 조회 시 exact host와 apex 후보를 모두 포함해서 findByDomainTailIn을 호출한다")
        void 배치조회는_exact와_apex_후보를_모두_포함한다() {
            String url = "https://someuser.tistory.com/123";
            given(curationLinkuRepository.findByCurationIdAndType(CURATION_ID, CurationLinkuType.EXTERNAL))
                    .willReturn(List.of(externalItem(url)));
            given(domainRepository.findByDomainTailIn(any())).willReturn(List.of());

            externalRecommendReader.read(CURATION_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<String>> tailsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(domainRepository).findByDomainTailIn(tailsCaptor.capture());
            assertTrue(tailsCaptor.getValue().containsAll(List.of("someuser.tistory.com", "tistory.com")));
        }

        @Test
        @DisplayName("아무 후보도 매칭되지 않으면 domain 필드는 null로 응답한다")
        void 매칭되는_후보가_없으면_domain_필드는_null이다() {
            String url = "https://unknown.example/post";
            given(curationLinkuRepository.findByCurationIdAndType(CURATION_ID, CurationLinkuType.EXTERNAL))
                    .willReturn(List.of(externalItem(url)));
            given(domainRepository.findByDomainTailIn(any())).willReturn(List.of());

            List<RecommendedLinkResponse> result = externalRecommendReader.read(CURATION_ID);

            assertEquals(1, result.size());
            assertNull(result.get(0).getDomain());
            assertNull(result.get(0).getDomainImageUrl());
        }
    }
}
