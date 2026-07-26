package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Image;
import com.umc.linkyou.domain.enums.CrawlStrategy;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "image_id")
    private Image image;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_strategy", length = 50)
    private CrawlStrategy crawlStrategy;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDomainTail(String domainTail) {
        this.domainTail = domainTail;
    }

    public void updateImage(Image image) {
        this.image = image;
    }
}
