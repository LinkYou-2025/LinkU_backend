package com.umc.linkyou.domain.classification;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "curation_ments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurationMent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curation_ment_id")
    private Long curationMentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Column(name = "header_text", columnDefinition = "TEXT", nullable = false)
    private String headerText;

    @Column(name = "footer_text", columnDefinition = "TEXT", nullable = false)
    private String footerText;
}
