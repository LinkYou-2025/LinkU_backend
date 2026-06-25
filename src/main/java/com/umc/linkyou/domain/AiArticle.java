package com.umc.linkyou.domain;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.mapping.UsersLinku;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_articles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiArticle extends BaseEntity {

    @Id
    @Column(name = "linku_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "situation_id", nullable = false)
    private Situation situation;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linku_id", nullable = false)
    private Linku linku;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "ai_feeling_id")
    private Long aiFeelingId;

    @Column(name = "ai_category_id")
    private Long aiCategoryId;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "img_url", columnDefinition = "TEXT")
    private String imgUrl;

    @Column(name = "keyword", columnDefinition = "TEXT")
    private String keyword;

    public void updateContent(String title, String summary, Long aiCategoryId, Long aiFeelingId, String imgUrl) {
        this.title = title;
        this.summary = summary;
        this.aiCategoryId = aiCategoryId;
        this.aiFeelingId = aiFeelingId;
        this.imgUrl = imgUrl;
    }
}
