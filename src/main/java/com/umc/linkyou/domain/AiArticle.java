package com.umc.linkyou.domain;

import jakarta.persistence.*;

import com.umc.linkyou.domain.common.BaseEntity;

import lombok.*;

@Entity
@Table(name = "ai_articles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_article_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linku_id", nullable = false, unique = true)
    private Linku linku;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
