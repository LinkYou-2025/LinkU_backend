package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "curation_linku")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurationLinku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long curationLinkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curation_id", nullable = false)
    private Curation curation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_linku_id", nullable = true)
    private UsersLinku usersLinku;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurationLinkuType type; // RECOMMENDED / EXTERNAL

    // 외부추천용 최소 컬럼
    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 255)
    private String title;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "url_normalized", length = 2048, insertable = false, updatable = false)
    private String urlNormalized;

    // 팩토리 메서드
    public static CurationLinku ofExternal(Curation curation, String url, String title, String imageUrl) {
        return CurationLinku.builder()
                .curation(curation)
                .type(CurationLinkuType.EXTERNAL)
                .url(url)
                .title(title)
                .imageUrl(imageUrl)
                .build();
    }

}