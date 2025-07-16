package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "curation_ment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurationMent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long curationMentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String headerText;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String footerText;
}
