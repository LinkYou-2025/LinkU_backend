package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class UsersFcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_fcm_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(name = "fcm_token", columnDefinition = "text", nullable = false)
    private String fcmToken;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    private static final int TOKEN_EXPIRE_DAYS = 60;

    public void activate() {
        this.isActive = true;
        this.lastUsedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS);
    }

    public void deactivate() {
        this.isActive = false;
        this.lastUsedAt = LocalDateTime.now();
    }
}
