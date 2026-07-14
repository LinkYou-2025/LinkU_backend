package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purposes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purposes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime selectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    public static Purposes of(String purpose, Users user) {
        Purposes entity = new Purposes();
        entity.purpose = purpose;
        entity.user = user;
        entity.selectedAt = LocalDateTime.now();
        return entity;
    }
}
