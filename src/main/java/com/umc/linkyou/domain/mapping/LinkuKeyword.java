package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "linku_keywords",
        uniqueConstraints = @UniqueConstraint(name = "uq_linku_keyword", columnNames = {"linku_id", "keyword_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LinkuKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linku_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linku_id", nullable = false)
    private Linku linku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;
}
