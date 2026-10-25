# 홈화면 링크 추천 스코어링

홈 추천은 사용자가 저장한 링크를 대상으로 다음 5개 요소의 가중합을 계산한다.
외부 AI 호출 없이 Native SQL로 점수를 계산하며, 가중치는 `application.yml`의
`recommend.home.score`에서 관리한다.

## Feature 벡터와 가중치

| Feature | 가중치 | 원본 데이터 | 계산 방식 |
|---|---:|---|---|
| EmotionMatch | 0.25 | `UsersLinku.emotion`, `isEmotionAi` | 요청 감정과의 유사도(`EmotionSimilarityUtil`, 0~60)를 60으로 나눔. AI 추론 감정이면 0.8배 |
| SituationMatch | 0.20 | `UsersLinku.situation`, `isSituationAi` | 요청 상황과 정확히 일치하면 1, 아니면 0. AI 추론 상황이면 0.8배 |
| PersonalEngagement | 0.25 | `UsersLinku.viewCount`, `lastViewedAt`, `createdAt` | 개인 조회 수 정규화와 오래 안 본 정도(staleness)의 평균 |
| Popularity | 0.20 | `Linku.totalViewCount` | 전체 조회 수의 로그 정규화, 최대 1 |
| CategoryMatch | 0.10 | `Linku.category`, 상황별 카테고리 매핑 | 요청 상황에 연결된 카테고리이면 1, 아니면 0. AI 신뢰도 감쇠 없음 |

가중치 합계는 1.0이다.
감정·상황이 일치하지 않는 링크도 후보에서 제외하지 않고 다른 요소로 점수를 받는다.

## 요청 처리 흐름

1. 사용자·감정·상황 존재 여부, 직업 설정 및 직업별 상황 매핑을 검증한다.
2. 저장 링크 수가 0이면 신규 사용자 오류, 3개 미만이면 추천 불가 오류를 반환한다.
3. 요청 상황에 연결된 카테고리 ID를 조회한다.
4. 사용자 저장 링크 전체에 5개 요소 점수를 계산하고 커서 조건·정렬·LIMIT을 적용한다.
5. 반환 후보의 폴더 매핑을 일괄 조회하고 최신 매핑의 폴더 이름으로 DTO를 생성한다.
6. 결과가 있으면 감정·상황 선택 통계를 별도 트랜잭션에서 동기로 저장한다.
7. `ApiResponse`로 `items`, `nextCursor`, `hasNext`를 반환한다.

## SQL 계산과 페이징

- `base` CTE: `users_linkus`, `linkus`, `domains`에서 해당 사용자의 후보와 응답 필드를 조회한다.
- `scored AS MATERIALIZED` CTE: 5개 요소의 가중합을 계산하고 `score_bucket`으로 변환한다.
- `score_bucket DESC, user_linku_id DESC` 순으로 정렬한다.
- 다음 페이지는 `(score_bucket, user_linku_id) < (커서 점수, 커서 ID)` 조건으로 조회한다.
- `size + 1`개를 조회해 `hasNext`를 판단한다. 기본 size는 5, 최대 20이다.

커서 페이징을 사용하더라도 매 요청마다 전체 저장 링크의 점수를 다시 계산한다.
오래 안 본 정도는 요청 시각을 기준으로 계산하므로 페이지 간 점수가 고정된 스냅샷은 아니다.

실제 DB 점수식은 `UsersLinkuRepositoryImpl#nativeScoreExpression`,
요청 흐름은 `LinkuRecommendService`, 메모리 내 점수 계산은 `HomeRecommendScoreService`에 있다.

## PersonalEngagement

```text
viewCountNorm = min(viewCount, 20) / 20
lastTouch = COALESCE(lastViewedAt, createdAt)
staleness = 1 - exp(-max(days(now - lastTouch), 0) / 14)
PersonalEngagement = (viewCountNorm + staleness) / 2
```

개인 조회 수가 높거나 오래 안 본 링크의 점수가 높아진다.
한 번도 보지 않았다면 저장 시각을 기준으로 계산한다. 수정 시각은 사용하지 않는다.

## Popularity

```text
Popularity = min(ln(1 + totalViewCount) / ln(1 + 1000), 1)
```

## TextMatch·KeywordMatch 제외

기존 TextMatch는 제목·AI 요약을 사용자 콘텐츠 프로필과 비교하고,
KeywordMatch는 링크 키워드와 사용자 키워드 프로필의 가중치 합을 계산했다.
사용자 프로필을 사전 계산해도 후보별 텍스트 비교와 키워드 집계는 매 요청마다 필요했다.

현재 홈 추천에서는 두 요소를 제외한다. 요청 경로에서 개인화 프로필 조회,
`to_tsvector`·`ts_rank_cd`·`similarity` 계산, 키워드 상관 서브쿼리,
`ai_articles` 조인을 제거했다.

사용처가 없어진 프로필 갱신 워커, 엔티티·리포지토리, 전용 집계 쿼리와
링크 저장 시 큐 등록도 제거했다. 프로필 재계산 배치와 큐 쓰기는 더 이상 실행되지 않는다.
기존 Flyway 이력과 DB 데이터 보존을 위해 과거 마이그레이션 및 프로필 테이블은 유지한다.
테이블 삭제가 필요하다면 별도 마이그레이션으로 진행한다.
향후 외부 콘텐츠 등으로 후보를 확장할 때는 후보를 좁힌 뒤 유사도를 계산하거나
사전 계산한 점수를 활용하는 방식을 검토한다.

## 추가 아이디어

- 요청 시간대와 날짜를 저장 시각과 비교하는 신호 검토
- 저장 링크 수별 실행 시간 및 실행계획 측정, 추천 클릭률 확인
