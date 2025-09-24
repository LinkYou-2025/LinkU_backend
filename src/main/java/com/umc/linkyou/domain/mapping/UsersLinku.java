package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Builder.Default
    @Column(name = "is_ai_exist", nullable = false)
    private Boolean isAiExist = false;
}
