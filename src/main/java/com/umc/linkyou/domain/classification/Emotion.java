package com.umc.linkyou.domain.classification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "emotion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Emotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emotion_id")
    private Long emotionId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;


}
