package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "auth_account",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "external_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private Provider provider;   // GOOGLE, KAKAO 등

    @Column(name = "external_id", nullable = false, columnDefinition = "TEXT")
    private String externalId;   // sub, kakao id 등

    @Column(name = "social_token", columnDefinition = "TEXT")
    private String socialToken;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    public void updateToken(String token) {
        this.socialToken = token;
    }
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
