package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users_linkus")
public class UsersLinku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_linku_id")
    private Long userLinkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Column(name = "memo")
    private String memo;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    // 연관관계: Users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateEmotion(Emotion emotion) {
        this.emotion = emotion;
    }

    public void markAiExist() {
        this.aiExist = true;
    }
}
