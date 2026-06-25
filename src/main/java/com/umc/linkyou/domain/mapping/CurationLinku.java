package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "curation_linkus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurationLinku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curation_linku_id")
    private Long curationLinkuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Curation curation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_linku_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UsersLinku usersLinku;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CurationLinkuType type; // RECOMMENDED / EXTERNAL

    // 외부추천용 최소 컬럼
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "title", length = 255)
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

    public static CurationLinku ofInternal(Curation curation, UsersLinku usersLinku, String imageUrl) {
        return CurationLinku.builder()
                .curation(curation)
                .usersLinku(usersLinku)
                .type(CurationLinkuType.RECOMMENDED)
                .url(usersLinku.getLinku().getLinkuUrl())
                .title(usersLinku.getLinku().getTitle())
                .imageUrl(imageUrl)
                .build();
    }

}
