package com.umc.linkyou.domain.recommend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 홈화면 추천 TextMatch용 유저별 title/summary 프로필 캐시.
 * 실제 갱신/조회는 {@code UserProfileMaterializer}(워커)와 {@code UserContentProfileRepository}의
 * native upsert 쿼리로 처리하고, 이 엔티티는 주로 단건 조회(findById)에 쓰인다.
 *
 * user_id 자체가 PK다(유저 1명당 프로필 1행). Users를 직접 참조하지 않는 이유는 이 테이블이
 * 도메인 엔티티가 아니라 추천 스코어링용 계산 캐시라서 — 코드 어디서도 여기서 Users 객체를
 * 다시 꺼내 쓸 일이 없다.
 *
 * @see com.umc.linkyou.service.common.HomeRecommendScoreService
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "user_content_profiles")
public class UserContentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    /** FTS용: "단어1 | 단어2 | ..." (유저의 저장 링크 title+summary 상위 빈도 단어) */
    @Column(name = "profile_tsquery_text", columnDefinition = "text")
    private String profileTsqueryText;

    /** trgm fallback용: title+summary 원문을 이어붙인 것(길이 캡) */
    @Column(name = "profile_text", columnDefinition = "text")
    private String profileText;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
