package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users_linku")
public class UsersLinku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userLinkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "situation_id")
    private Situation situation;

    @Column(name = "title", length = 255)
    private String title;

    private String memo;

    @Column(columnDefinition = "text")
    private String imageUrl;

    // 연관관계: Users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 연관관계: Linku
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linku_id", nullable = false)
    private Linku linku;

    @OneToMany(mappedBy = "usersLinku", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinkuFolder> linkuFolders = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_ai_exist", nullable = false)
    private Boolean aiExist = false;

    public Boolean getAiExist() {
        return this.aiExist;
    }

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;
}
