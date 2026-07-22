package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "curation_section_infos",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_curation_section",
                columnNames = {"base_month", "section_number"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurationSectionInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curation_section_info_id")
    private Long id;

    @Column(name = "base_month", length = 7, nullable = false)
    private String month; // "YYYY-MM"

    @Column(name = "section_number", nullable = false)
    private int sectionNumber; // 1, 2, 3

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
}
