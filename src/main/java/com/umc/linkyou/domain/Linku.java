package com.umc.linkyou.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.mapping.UsersLinku;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "linkus")
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

    @Column(name = "linku_url", columnDefinition = "text", nullable = false)
    private String linkuUrl;

    @OneToOne(mappedBy = "linku", fetch = FetchType.LAZY)
    private AiArticle aiArticle;

    @Column(columnDefinition = "text", nullable = false)
    private String title;

    @OneToMany(mappedBy = "linku", cascade = CascadeType.ALL)
    @Builder.Default
    private List<UsersLinku> usersLinku = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "linku", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinkuKeyword> linkuKeywords = new ArrayList<>();

    @Column(name = "img_url", columnDefinition = "TEXT")
    private String imgUrl;

    @Builder.Default
    @Column(name = "total_view_count", nullable = false)
    private long totalViewCount = 0L;

    // AI 캐시용 감정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    // AI 캐시용 상황
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "situation_id", nullable = false)
    private Situation situation;

    public void updateCategory(Category category) {
        this.category = category;
    }

    public void updateUrl(String linkuUrl) {
        this.linkuUrl = linkuUrl;
    }

    public void updateDomain(Domain domain) {
        this.domain = domain;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", length = 20)
    private PageType pageType;

    @Column(name = "page_type_checked_at")
    private LocalDateTime pageTypeCheckedAt;

    public void updatePageType(PageType pageType, LocalDateTime checkedAt) {
        this.pageType = pageType;
        this.pageTypeCheckedAt = checkedAt;
    }

    public boolean isSummarizable() {
        return pageType == null || pageType == PageType.CONTENT;
    }

    public boolean needsPageTypeRecheck(LocalDateTime threshold) {
        return !isSummarizable()
                && pageTypeCheckedAt != null
                && pageTypeCheckedAt.isBefore(threshold);
    }

    public void updateEmotion(Emotion emotion) {
        this.emotion = emotion;
    }

    public void updateSituation(Situation situation) {
        this.situation = situation;
    }
}
