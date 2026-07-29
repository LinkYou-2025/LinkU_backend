package com.umc.linkyou.domain.recommend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 홈화면 추천 KeywordMatch용 유저별 상위 키워드(태그) 빈도 캐시.
 * 후보 링크의 linku_keywords와 스칼라 서브쿼리로 겹쳐서 KeywordMatch 점수를 계산하는 데 쓴다.
 * 지금은 "이 유저가 평소 저장하는 키워드"라는 intra-user 신호이고, 협업 필터링(inter-user)용으로
 * 확장할 때는 이 테이블을 건드리지 않고 별도 feature로 추가한다 (service/common/README.md 참고).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "user_profile_keywords",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_profile_keyword", columnNames = {"user_id", "keyword_id"}))
public class UserProfileKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_keyword_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "weight", nullable = false)
    private int weight;
}
