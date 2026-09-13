package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Purposes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Users - Purposes 다대다 관계의 조인 엔티티
@Entity
@Table(
        name = "users_purposes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_users_purposes_user_purpose",
                        columnNames = {"user_id", "purpose_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UsersPurpose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purpose_id", nullable = false)
    private Purposes purpose;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    public static UsersPurpose of(Users user, Purposes purpose) {
        return UsersPurpose.builder()
                .user(user)
                .purpose(purpose)
                .selectedAt(LocalDateTime.now())
                .build();
    }
}
