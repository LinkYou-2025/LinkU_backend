package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Purpose;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "purposes")
public class Purposes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "purpose_id")
    private Long id;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 외래키 설정
    @OnDelete(action = OnDeleteAction.CASCADE)
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
