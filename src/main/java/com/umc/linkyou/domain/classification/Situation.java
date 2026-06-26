package com.umc.linkyou.domain.classification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "situations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Situation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "situation_id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;
}
