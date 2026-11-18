package com.umc.linkyou.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.umc.linkyou.domain.common.BaseEntity;

import lombok.*;

@Entity
@Table(name = "curations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Curation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curation_id")
    private Long curationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(name = "base_month", length = 7, nullable = false)
    private String baseMonth;

    @Column(name = "header_ment", columnDefinition = "TEXT")
    private String headerMent;

    @Column(name = "footer_ment", columnDefinition = "TEXT")
    private String footerMent;

    public void updateMent(String headerMent, String footerMent) {
        this.headerMent = headerMent;
        this.footerMent = footerMent;
    }
}
