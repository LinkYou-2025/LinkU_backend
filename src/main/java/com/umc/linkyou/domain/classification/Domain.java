package com.umc.linkyou.domain.classification;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.umc.linkyou.domain.enums.DomainSource;

import lombok.*;

@Entity
@Table(name = "domains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Domain {

    @Id
    @Column(name = "domain_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long domainId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "domain_tail", length = 255, nullable = false)
    private String domainTail;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    // 사람이 관리하는 행(MANUAL)인지, 처음 보는 사이트라 자동 등록된 행(AUTO)인지
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    @Builder.Default
    private DomainSource source = DomainSource.MANUAL;

    @Column(name = "favicon_next_attempt_at")
    private LocalDateTime faviconNextAttemptAt;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDomainTail(String domainTail) {
        this.domainTail = domainTail;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateFavicon(String imageUrl) {
        this.imageUrl = imageUrl;
        this.faviconNextAttemptAt = null;
    }

    public void markFaviconFailed(LocalDateTime nextAttemptAt) {
        this.faviconNextAttemptAt = nextAttemptAt;
    }
}
