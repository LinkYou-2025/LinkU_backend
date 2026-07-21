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


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "users_linkus")
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
    @Builder.Default
    private List<LinkuFolder> linkuFolders = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_ai_exist", nullable = false)
    private Boolean aiExist = false;

    @Builder.Default
    @Column(name = "is_emotion_ai", nullable = false)
    private Boolean emotionAi = true;

    @Builder.Default
    @Column(name = "is_situation_ai", nullable = false)
    private Boolean situationAi = true;

    public Boolean getAiExist() {
        return this.aiExist;
    }

    public void markAiExist(boolean aiExist) {
        this.aiExist = aiExist;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateEmotion(Emotion emotion) {
        this.emotion = emotion;
    }

    public void updateEmotionAi(boolean emotionAi) {
        this.emotionAi = emotionAi;
    }

    public void updateSituation(Situation situation) {
        this.situation = situation;
    }

    public void updateSituationAi(boolean situationAi) {
        this.situationAi = situationAi;
    }

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;
}
