package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Interest;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "interests")
public class Interests {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "interest_id")
    private Long id;

    @Column(name = "interest", nullable = false)
    private String interest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 외래키 설정
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    public Interests() {

    }

    public Interests(String enumInterest, Users newUser) {
        this.interest = enumInterest;
        this.user = newUser;
        this.selectedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.selectedAt == null) {
            this.selectedAt = LocalDateTime.now();
        }
    }

}
