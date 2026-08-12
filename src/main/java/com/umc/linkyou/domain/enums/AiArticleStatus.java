package com.umc.linkyou.domain.enums;

// AiArticle 생성 상태. POST가 크롤링+Gemini 호출을 비동기로 처리하면서,
// GET에서 이 상태를 보고 프론트가 폴링을 계속할지(PENDING) 멈출지(DONE/FAILED) 판단한다.
public enum AiArticleStatus {
    PENDING, // 생성 요청 접수, 크롤링/Gemini 호출 진행 중
    DONE,    // 생성 완료, summary 사용 가능
    FAILED   // 생성 실패 (robots.txt 차단, 크롤링 실패, Gemini 오류 등). failReason 참고
}
