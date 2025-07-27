package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Situation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "situation_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SituationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "situation_id", nullable = false)
    private Situation situation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
