package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AiArticleStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_articles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_article_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linku_id", nullable = false, unique = true)
    private Linku linku;

    // PENDING 상태에서는 아직 채워지지 않으므로 nullable.
    @Column(name = "summary", length = 255)
    private String summary;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiArticleStatus status = AiArticleStatus.PENDING;

    // 실패 사유 코드 (예: CRAWLER4031, GEMINI5041). status=FAILED일 때만 값이 있다.
    @Column(name = "fail_reason", length = 50)
    private String failReason;

    // 생성 요청 접수 시점에 쓰는 정적 팩터리. summary는 아직 없고 status=PENDING으로 시작한다.
    public static AiArticle pending(Linku linku) {
        return AiArticle.builder()
                .linku(linku)
                .status(AiArticleStatus.PENDING)
                .build();
    }

    // 비동기 생성이 성공적으로 끝났을 때 호출한다.
    public void complete(String summary) {
        this.summary = summary;
        this.status = AiArticleStatus.DONE;
        this.failReason = null;
    }

    // 비동기 생성이 실패했을 때 호출한다. failReasonCode는 AiArticleErrorStatus/GeminiErrorStatus 등의 code 값.
    public void markFailed(String failReasonCode) {
        this.status = AiArticleStatus.FAILED;
        this.failReason = failReasonCode;
    }

    // FAILED 상태에서 재시도할 때 PENDING으로 되돌린다.
    public void restartPending() {
        this.status = AiArticleStatus.PENDING;
        this.failReason = null;
    }
}
