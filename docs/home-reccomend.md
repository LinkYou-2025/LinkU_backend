# 홈화면 링크 추천 스코어링

추천 점수: scoreBucket

EmotionMatch: 현재 추천하려는 링크의 감정이랑, input으로 넣은 감정 유사도
Situtation: 현재 추천하려는 링크의 상황이랑, input으로 넣은 상황 유사도
PersonalEngagement: 사용자가 어떤 링크를 본 viewcount랑 마지막으로 본 날짜를 가지고 계산한 점수
Popularity: 어떤 링크를 여러 사용자가 몇번 봤는지 계산한 점수
TextMatch: title, summary가 사용자가 좋아하는 거 계산해서 모아놓은정보( 유저프로필)과 유사한지
KeywordMatch:  유저 프로필과 keyword 유사도 점수
CategoryMatch: SituationCategory점수

추가 아이디어)
- 요청시간대와 날짜를 users_linku created_at 비교


## Feature 벡터

| # | Feature | 원본 컬럼 | 계산 위치 | 상태 |
|---|---|---|---|---|
| 1 | EmotionMatch | `UsersLinku.emotion` | 실시간 SQL (CASE WHEN, `EmotionSimilarityUtil` 재사용) | ✅ 구현 완료 |
| 2 | SituationMatch | `UsersLinku.situation` | 실시간 SQL (저장 당시 situation == 요청 situationId면 1.0, 아니면 0) | ✅ 구현 완료 |
| 3 | PersonalEngagement | `UsersLinku.viewCount`, `lastViewedAt` | 실시간 SQL (viewCount 캡 정규화 + lastViewedAt 지수감쇠 평균) | ✅ 구현 완료 |
| 4 | Popularity | `Linku.totalViewCount` | 실시간 SQL (로그 정규화 + 캡) | ✅ 구현 완료 |
| 5 | TextMatch | `Linku.title`, `AiArticle.summary` | 사전계산된 유저 프로필과 Postgres FTS 매칭 (trgm fallback) | ✅ 구현 완료 |
| 6 | KeywordMatch | `LinkuKeyword`/`Keyword` | 사전계산된 유저 키워드 프로필과 스칼라 서브쿼리 매칭 | ✅ 구현 완료 |
| 7 | CategoryMatch | `Linku.category` | 실시간 SQL (situation→category 매핑(`SituationCategoryService`)에 걸리면 1.0, 아니면 0) | ✅ 구현 완료 |

## Novelty Quota (최근에 안 본 것 우선 노출) — 구현 완료

7축 가중합과는 별개로, 한 페이지를 구성할 때 "최근에 안 본" 후보를 quota만큼 먼저 채우고 나머지를 기존
가중합(normal)으로 채우는 2단계 조립을 추가했다. 7축에 새 feature를 추가한 게 아니라, DB 쿼리 결과를
서비스 레이어에서 두 풀로 나눠 합치는 post-process 단계다.

- **novelty 조건**: `COALESCE(usersLinku.lastViewedAt, usersLinku.createdAt) < now - recencyThresholdDays`.
  `lastViewedAt`이 있으면 마지막으로 본 지 기준일이 넘었는지, null(한 번도 안 봄)이면 저장한 지(`createdAt`)
  기준일이 넘었는지를 같은 기준으로 본다 — 방금 저장해서 아직 볼 기회가 없었던 링크가 novelty로 잘못
  잡히는 것을 막기 위해서다. `viewCount`는 저장 시점에 항상 0으로 초기화되고(`LinkuConverter.toUsersLinku`가
  `viewCount`/`lastViewedAt`을 세팅하지 않음), `incrementViewCount()`가 호출될 때만 `lastViewedAt`이 채워지므로
  `viewCount=0` 조건은 `lastViewedAt IS NULL`과 동치라 별도로 쓰지 않는다.
- **novelty 버킷 정렬**: 7축이 아니라 EmotionMatch/SituationMatch 두 축만으로 정렬한다
  (`HomeRecommendScoreService#noveltyContextScoreExpression`, 기존 `emotionMatchExpression`/
  `situationMatchExpression`을 그대로 재사용하므로 AI 추론 신뢰도 감쇠도 동일하게 적용됨).
- **quota**: `RecommendScoreProperties.Novelty.quotaRatio`(기본 0.3)로 한 페이지(`size`)당 목표 개수를 정한다.
  목표치일 뿐이라 novelty 후보가 모자라면 normal 버킷이 초과해서 채운다(`LinkuRecommendService
  #fetchNoveltyAndNormalCandidates`).
- **두 버킷은 서로소로 유지된다**: `findNormalRecommendCandidates`가 novelty 조건을 제외(`NOT COALESCE(...) <
  threshold`)한 채로 조회하므로, 같은 링크가 novelty와 normal 양쪽에서 중복으로 뽑히는 일이 없다.
  `findHomeRecommendCandidates`(novelty 필터 없는 원본)는 그대로 남겨뒀고, 기존 테스트/호출부와의
  하위호환을 위해 시그니처를 바꾸지 않았다 — normal 버킷은 별도 메서드(`findNormalRecommendCandidates`)로
  분리했다.
