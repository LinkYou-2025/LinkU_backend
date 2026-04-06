package com.umc.linkyou.domain.log;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.KeywordType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "keyword_monthly_count",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_keyword_monthly",
        columnNames = {"user_id", "type", "ref_id", "base_month"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KeywordMonthlyCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_monthly_count_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private KeywordType type;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "base_month", length = 7, nullable = false)
    private String baseMonth;

    @Column(nullable = false)
    private int count;
}
