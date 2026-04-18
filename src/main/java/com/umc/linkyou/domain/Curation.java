package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "curation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Curation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long curationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(length = 7, nullable = false)
    private String month;

    @Column(columnDefinition = "TEXT")
    private String headerMent;

    @Column(columnDefinition = "TEXT")
    private String footerMent;

    public void updateMent(String headerMent, String footerMent) {
        this.headerMent = headerMent;
        this.footerMent = footerMent;
    }
}