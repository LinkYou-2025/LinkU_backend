package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String interest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private LocalDateTime selectedAt;

    public static Interests of(String interest, Users user) {
        Interests entity = new Interests();
        entity.interest = interest;
        entity.user = user;
        entity.selectedAt = LocalDateTime.now();
        return entity;
    }
}
