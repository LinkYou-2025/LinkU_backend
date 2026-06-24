package com.umc.linkyou.domain;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.mapping.UsersLinku;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "linku")
public class Linku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long linkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(name = "linku", columnDefinition = "text", nullable = false)
    private String linkuUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aiArticle_id")
    private AiArticle aiArticle;

    @Column(columnDefinition = "text", nullable = false)
    private String title;

    @OneToMany(mappedBy = "linku", cascade = CascadeType.ALL)
    private List<UsersLinku> usersLinku = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "linku", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinkuKeyword> linkuKeywords = new ArrayList<>();

    @Column(name = "img_url", columnDefinition = "TEXT")
    private String imgUrl;

    @Builder.Default
    @Column(name = "total_view_count", nullable = false)
    private long totalViewCount = 0L;
}
