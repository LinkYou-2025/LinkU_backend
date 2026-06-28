package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "footer_tex