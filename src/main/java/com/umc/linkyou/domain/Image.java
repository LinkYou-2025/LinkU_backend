package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.ImageSourceType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 이미지 저장 방식(Object Key-only 저장)을 위한 엔티티.
 *
 * <p>두 가지 출처의 이미지를 하나의 테이블에서 구분해 관리한다.
 * <ul>
 *     <li>{@link ImageSourceType#S3} - 우리 S3/CloudFront에 업로드된 이미지. {@code location}에는
 *     CloudFront 상대 경로("/"로 시작하는 object key)만 저장한다.</li>
 *     <li>{@link ImageSourceType#EXTERNAL} - 링크 크롤링 등으로 얻은 외부 사이트 이미지. {@code location}에는
 *     외부 이미지의 전체 URL을 그대로 저장한다.</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "images")
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private ImageSourceType sourceType;

    @Lob
    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    private String location; // S3 key(항상 "/"로 시작) 또는 외부 이미지 URL

    public static Image ofS3(String key) {
        return Image.builder()
                .sourceType(ImageSourceType.S3)
                .location(normalizeS3Key(key))
                .build();
    }

    public static Image ofExternal(String url) {
        return Image.builder()
                .sourceType(ImageSourceType.EXTERNAL)
                .location(url)
                .build();
    }

    public boolean isS3() {
        return sourceType == ImageSourceType.S3;
    }

    public void updateLocation(String key) {
        this.location = sourceType == ImageSourceType.S3 ? normalizeS3Key(key) : key;
    }

    private static String normalizeS3Key(String key) {
        if (key == null) return null;
        return key.startsWith("/") ? key : "/" + key;
    }
}
