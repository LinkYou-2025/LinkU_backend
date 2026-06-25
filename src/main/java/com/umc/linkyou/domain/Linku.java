package com.umc.linkyou.domain;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.mapping.UsersLinku;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "linkus")
public class Linku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linku_id")
    private Long linkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(name = "linku", columnDefinition = "text", nullable = false)
    private String linku;

    @OneToOne(mappedBy = "linku", fetch = FetchType.LAZY)
    private AiArticle aiArticle;

    @Column(name = "title", columnDefinition = "text", nullable = false)
    private String title;

    @OneToMany(mappedBy = "linku", cascade = CascadeType.ALL)
    private List<UsersLinku> usersLinku = new ArrayList<>();

    @Builder.Default
    @Column(name = "total_view_count", nullable = false)
    private long totalViewCount = 0L;

    public void updateCategory(Category category) {
        this.category = category;
    }

    public void updateDomain(Domain domain) {
        this.domain = domain;
    }

    public void updateUrl(String linku) {
        this.linku = linku;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateAiArticle(AiArticle aiArticle) {
        this.aiArticle = aiArticle;
    }
}
