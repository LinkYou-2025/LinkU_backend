package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Purpose;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purposes")
public class Purposes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime selectedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // 외래키 설정
    private Users user;

    public Purposes() {

    }

    public Purposes(String enumPurpose, Users newUser) {
        this.purpose = enumPurpose;
        this.user = newUser;
        this.selectedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        System.out.println("[PREPERSIST] selectedAt = " + this.selectedAt);
        if (this.selectedAt == null) {
            this.selectedAt = LocalDateTime.now();
        }
    }

}
