package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.enums.CrawlStrategy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_strategy", length = 50)
    private CrawlStrategy crawlStrategy;
}
