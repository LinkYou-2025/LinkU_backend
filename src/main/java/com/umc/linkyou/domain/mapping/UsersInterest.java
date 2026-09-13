package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Users - Interests 다대다 관계의 조인 엔티티
@Entity
@Table(
        name = "users_interests",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_users_interests_user_interest",
                        columnNames = {"user_id", "interest_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UsersInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interests interest;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    public static UsersInterest of(Users user, Interests interest) {
        return UsersInterest.builder()
                .user(user)
                .interest(interest)
                .selectedAt(LocalDateTime.now())
                .build();
    }
}
