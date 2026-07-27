# 홈화면 링크 추천 스코어링 리팩터링

`LinkuRecommendService`(홈화면 링크 추천)의 스코어링 로직을 재설계하는 작업 기록. 큐레이션 내부/외부 추천
(`service.curation.recommend.*`)과는 완전히 별개의 독립 로직이며, 이 문서는 홈화면 추천에만 해당한다.

## 배경

기존 로직은 저장된 신호(감정, situation, 조회수, 최신성, 태그, 요약 등) 대부분을 DB엔 쌓아두고도 실제 랭킹에는
`UsersLinku.emotion`과 `Linku.category`(situation 매핑) 두 축만 썼다. 그 결과 랭킹 품질이 이 두 축의 조합 수만큼만
갈릴 수 있는 얕은 rule-based ranker였고, 정렬/페이징도 in-memory로 처리해 유저의 저장 링크 수가 늘면 효율이
떨어지는 구조였다(→ `0aa4eee refactor #319 home recommend n+1`에서 QueryDSL 기반 DB 계산으로 먼저 개선).

이번 작업의 방향은 "content-based + weak context 추천"을 지금 만들고, "collaborative(교차 유저) 추천"은 나중에
feature 하나를 더 추가하는 형태로 확장 가능하게 설계해두는 것.

## 아키텍처 원칙

- **score = wᵀx** : 가중치 벡터 `w`와 0~1로 정규화된 feature 벡터 `x`의 내적. 각 feature는 독립적으로 관리하고,
  최종 점수는 벡터 계산 한 줄로 처리한다. 새 feature를 추가할 때 벡터/가중치에 항목 하나만 늘리면 되게 유지한다.
- **가중치·정규화 상수는 설정으로 분리** : `RecommendScoreProperties`(`application.yml: recommend.home.score.*`).
  코드 재배포 없이 튜닝 가능해야 한다.
- **DB에서 실시간 계산 가능한 신호 vs 사전계산이 필요한 신호를 구분**한다. 전자(스칼라 컬럼 기반)는 QueryDSL
  CASE WHEN/템플릿 식으로 그대로 확장하고, 후자(텍스트/키워드처럼 비교 대상이 필요한 신호)는 비동기로
  미리 계산해 별도 테이블에 저장한 뒤 요청 시점엔 읽기만 한다. 요청마다 무거운 계산을 하지 않는 것이 이번
  리팩터링의 핵심 목표이므로, 이 경계를 깨는 변경은 지양한다.

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
| 8 | DomainDiversity | `Linku.domain` | 가중합이 아닌 결과 재정렬(post-process) | ⏸ 보류 |
| 9 | Collaborative(교차 유저) | (미정 — 다른 유저의 저장/키워드 데이터) | 미정 | ⏸ 향후 확장 축, 설계 전 |

1~4, 7은 전부 이미 있는 스칼라 컬럼이라 새 테이블 없이 구현됐다. 5~6은 "비교 대상(유저 프로필)"이 필요해서
사전계산 인프라(마이그레이션 + 워커)까지 포함해 구현했다. 8, 9는 이번 스코프에서 의도적으로 제외했다.

> **변경 이력**: 원래 SituationMatch 하나가 "직접일치 1.0 / situation→category 매핑 0.6"을 함께 담당했으나,
> (1) situation은 48종이라 direct match가 오히려 드문 케이스라 매핑 fallback이 사실상 주력 신호였고,
> (2) situationAi 신뢰도 감쇠가 "저장 당시 태깅"과 무관한 category 신호에도 함께 곱해지는 개념적 불일치가
> 있어서, CategoryMatch를 완전히 독립된 7번째 axis로 분리했다. SituationMatch는 이제 직접 일치만 보고
> (0/1.0, situationAi 감쇠 적용), CategoryMatch는 콘텐츠 속성(Linku.category)만 보는 순수 신호라 감쇠를
> 적용하지 않는다. 가중치는 기존 situation 0.25를 situation 0.15 + category 0.10으로 나눴다(둘 다 시작값,
> 실측 튜닝 필요).

## 구현된 것

- `config/properties/RecommendScoreProperties.java` — 가중치(emotion/situation/engagement/popularity/text/keyword/category) +
  정규화 상수(viewCountCap, recencyHalfLifeDays, popularityViewCountCap, keywordWeightCap) +
  신뢰도 상수(`Confidence.aiEmotionDiscount`/`aiSituationDiscount`).
- `application.yml`의 `recommend.home.score.*` — 시작값일 뿐 실측 튜닝 필요.
- `service/common/HomeRecommendScoreService.java` — Java 메모리용(`score`, `FeatureVector`)과 QueryDSL
  표현식용(`scoreExpression` 등) 두 형태로 동일한 공식을 제공. feature 벡터는 7차원
  (EmotionMatch/SituationMatch/PersonalEngagement/Popularity/TextMatch/KeywordMatch/CategoryMatch). 감정 유사도
  공식은 여전히 `EmotionSimilarityUtil` 하나가 진실 공급원. EmotionMatch/SituationMatch는 `UsersLinku.emotionAi`/
  `situationAi`(AI 추론 여부)에 따라 신뢰도 감쇠가 추가로 곱해진다(아래 "AI vs 유저 직접 분류 신뢰도 가중치" 참고).
  CategoryMatch는 저장 당시 태깅이 아닌 콘텐츠 속성(`Linku.category`)만 보는 신호라 이 감쇠를 적용하지 않는다.
- `domain/recommend/` — `UserContentProfile`(TextMatch용, PK=user_id), `UserProfileKeyword`(KeywordMatch용,
  synthetic PK + `(user_id, keyword_id)` unique), `UserProfileRefreshQueue`(dirty queue, PK=user_id) 엔티티.
- `repository/recommend/` — 위 3개 엔티티의 리포지토리. upsert는 전부 Postgres `ON CONFLICT` native 쿼리로
  처리한다(`KeywordRepository.insertIgnore`와 같은 패턴).
- `repository/mapping/LinkuKeywordRepository#findKeywordFrequencyByUserId` — 유저별 키워드 빈도 집계(JPQL GROUP BY).
- `repository/dto/UserKeywordWeightRow` — 위 집계 결과 projection.
- `repository/UserLinkuRepository/UsersLinkuRepositoryCustom`, `Impl` —
  - `findHomeRecommendCandidates`가 situationId/now/profileTsqueryText/profileText를 받아
    `HomeRecommendScoreService`에 스코어링을 위임한다. `usersLinku.situation`이 nullable이라 명시적으로
    `.leftJoin(usersLinku.situation)`을 걸어야 한다(안 그러면 JPQL이 암시적 INNER JOIN으로 해석해 situation
    없는 저장 링크가 결과에서 통째로 빠지는 버그 발생). TextMatch 계산을 위해 `linku.aiArticle`도 LEFT JOIN 추가.
  - `findRecentContentForProfile` — `UserProfileRefreshWorker`가 프로필 재계산 재료(title+summary)를
    가져올 때 쓰는 신규 메서드.
- `service/Linku/LinkuRecommendService.java` — 요청 처리 전에 `UserContentProfile`을 유저ID로 단건 조회해서
  profileTsqueryText/profileText를 새 시그니처에 넘긴다.
- `service/Linku/LinkuCreateService.java` — `createUsersLinku()`에서 저장이 끝나면
  `UserProfileRefreshQueueRepository.enqueue(user.getId())`로 재계산 대상을 표시한다(링크가 새로 생겼든
  기존 링크를 재저장했든 상관없이, 이 유저의 저장 목록이 바뀔 때마다 항상 표시된다).
- `service/common/UserProfileRefreshWorker.java` — `@Scheduled(cron = "0 */5 * * * *")`로 큐를 chunk(200명)
  단위로 드레인한다. title+summary는 Java 정규식 토큰화(`[^\p{L}\p{N}]+` 분리 + 소문자화, Postgres `'simple'`
  설정과 거의 동일한 수준)로 상위 20개 단어를 뽑아 `profile_tsquery_text`에, 원문은 길이 캡(4000자)을 두고
  `profile_text`에 저장한다. 키워드는 `LinkuKeywordRepository#findKeywordFrequencyByUserId`로 상위 20개를
  집계해 `user_profile_keywords`에 반영한다. 실패한 유저는 큐에서 지우지 않고 다음 드레인 때 재시도한다.
- `db/migration/V12__add_recommend_content_profile.sql` — `pg_trgm` 확장 + 위 3개 테이블.

> **네트워크 제약 안내**: 이 코드는 Gradle/Maven 저장소 접근이 막힌 샌드박스 환경에서 작성되어
> `./gradlew compileJava`로 실제 컴파일 검증을 못 했다. 특히 QueryDSL API 사용부(`NumberExpression.doubleValue()`,
> `JPAExpressions` 스칼라 서브쿼리를 `Expressions.numberTemplate` 인자로 넘기는 부분)는 공식 API 기준으로
> 신중히 검토했지만, 로컬에서 `./gradlew compileJava` 한 번 돌려서 확인이 꼭 필요하다.

## TextMatch 스코어링 상세

후보 링크의 `to_tsvector('simple', title || ' ' || COALESCE(summary,''))`와 저장해둔
`to_tsquery('simple', profile_tsquery_text)`로 `ts_rank_cd`를 계산하고, `rank / (rank + 1)`로 0~1에 눌러 담는다.
이 값이 0이면(조사 등으로 토큰 형태가 달라져 정확히 겹치는 단어가 없으면) `similarity(후보텍스트, profile_text)`
(pg_trgm)를 대신 쓰되 신뢰도가 낮으므로 ×0.7 감쇠해서 쓴다.

```
textMatch = ftsRank > 0 ? ftsRank : trgmSimilarity * 0.7
```

## KeywordMatch 스코어링 상세

후보 링크의 `linku_keywords`와 그 유저의 `user_profile_keywords`를 **스칼라 서브쿼리**로 겹쳐서 weight 합을
구하고, `keywordWeightCap`으로 정규화한다. JOIN + GROUP BY로 하면 메인 쿼리의 fetch join 구조(행당 1건 보장)가
깨지므로 서브쿼리로 처리해 메인 쿼리 shape을 그대로 유지한다.

`KeywordMatch`(#6)는 "이 유저가 평소 저장하는 키워드와 이 후보가 얼마나 겹치는가"라는 **유저 내부(intra-user)**
신호다. 나중에 진짜 협업 필터링을 넣을 때는 "유저 A와 유저 B가 겹치는 키워드/링크를 얼마나 저장했는가" 같은
**유저 간(inter-user)** 신호가 필요한데, 이건 완전히 다른 계산(다른 유저 데이터 접근, 후보군 생성 방식 자체의
변경)이라 별개의 feature(#8)로 벡터에 추가하는 쪽으로 남겨둔다. 지금의 `user_profile_keywords`는 그대로 두고
건드리지 않는다.

## AI vs 유저 직접 분류 신뢰도 가중치

`UsersLinku`는 저장 당시 감정/situation이 유저가 직접 고른 값인지, AI가 추론해 채운 값인지를
`emotionAi`/`situationAi`(Boolean, true=AI 추론)로 이미 구분해 갖고 있었다. 이 신호를 스코어링에 반영해서,
유저가 직접 고른 라벨은 그대로 신뢰하고 AI가 추론한 라벨은 `Confidence.aiEmotionDiscount`/
`aiSituationDiscount`(기본값 0.8)만큼 점수를 깎는다.

```
emotionMatch    = base(EmotionSimilarityUtil) / 60 * (emotionAi   ? aiEmotionDiscount   : 1.0)
situationMatch  = (직접일치 ? 1.0 : 0.0)                * (situationAi ? aiSituationDiscount : 1.0)
categoryMatch   = (situation→category 매핑 일치 ? 1.0 : 0.0)   # 감쇠 없음 — 콘텐츠 속성 신호라 AI/유저 태깅과 무관
```

Java 메모리 버전(`emotionMatch`/`situationMatch`)은 `candidateEmotionIsAi`/`candidateSituationIsAi` boolean
파라미터를 추가로 받고, QueryDSL 버전(`emotionMatchExpression`/`situationMatchExpression`)은 정규화된 점수에
`CASE WHEN usersLinku.emotionAi = true THEN :discount ELSE 1.0 END` 형태의 감쇠 factor를 곱해서 처리한다 —
둘 다 별도 컬럼/조인 추가 없이 이미 존재하는 `emotionAi`/`situationAi` 컬럼만 읽는다. `categoryMatch`/
`categoryMatchExpression`은 이 감쇠 로직 자체가 없다 — `Linku.category`는 저장 시점 유저/AI 태깅과 무관한
콘텐츠 고유 속성이라서.

**보류: 요청 시점 실시간 날씨/시간대 신호.** 유저가 함께 제안한 "추천 시점의 날씨/시각을 체크"하는 아이디어는
지금 반영하지 않았다. 이유는 두 가지다. (1) 동기 요청 경로에서 외부 날씨 API를 직접 호출하면 블로킹 I/O가
추천 응답 지연에 그대로 얹히는데, 이건 이 리팩터링이 피하려던 패턴과 정확히 반대 방향이다 — 하려면 날씨를
주기적으로 캐싱해 "현재 날씨 버킷"만 읽는 비동기 갱신 구조가 먼저 필요하다(위 dirty-queue/워커 패턴과 유사).
(2) 날씨를 반영하려면 "이 카테고리/콘텐츠가 어떤 날씨에 적합한가"라는 콘텐츠 태그 체계 자체가 아직 없어서,
신호를 넣어도 매핑할 대상이 없다. 요일/시각(day-of-week/time-of-day)은 `UsersLinku.createdAt`만으로 실시간
SQL 식(`EXTRACT(DOW/HOUR FROM now())`)으로 저렴하게 넣을 수 있어 날씨보다 우선순위가 높지만, 이 역시 효과가
검증 안 된 가설이라 별도 트랙으로 미룬다(다음 액션 아이템 참고).

## 테스트

- `src/test/.../service/common/HomeRecommendScoreServiceTest.java` — Java 메모리 스코어링 단위 테스트
  (emotionMatch/situationMatch/categoryMatch/personalEngagement/popularity 각각의 경계값 + score() 가중합
  검증). Spring 컨텍스트 없이 `RecommendScoreProperties`를 직접 생성해 순수 POJO로 테스트한다. emotionMatch/
  situationMatch는 AI 추론(`candidateEmotionIsAi`/`candidateSituationIsAi` = true) 시
  `aiEmotionDiscount`/`aiSituationDiscount`(0.8)만큼 정확히 감쇠되는지도 검증한다. categoryMatch는 감쇠가
  없다는 것 자체가 검증 포인트라 별도 nested class로 분리해뒀다.
- `src/test/.../repository/UserLinkuRepository/UsersLinkuRepositoryImplTest.java` — `findHomeRecommendCandidates`
  통합 테스트(Testcontainers Postgres). SituationMatch 직접일치(situation 0.15) > CategoryMatch만 일치(category
  0.10) > 매칭 없음(0) 순서, situation=null인 후보가 결과에서 빠지지 않는지(LEFT JOIN 회귀 검증),
  PersonalEngagement/Popularity가 높을수록 상위로 오는지를
  검증한다. profileTsqueryText/profileText는 둘 다 null로 넘겨서 pg_trgm/FTS 함수 호출 자체가 생략되게 했다
  (textMatchExpression의 null 체크가 Java 레벨이라 SQL에 similarity()/ts_rank_cd가 아예 안 들어감).
  **EmotionMatch는 이 통합 테스트에서 검증하지 않는다** — 아래 "발견된 이슈" 참고.
- QueryDSL 표현식(`*Expression` 메서드) 자체의 SQL 값 검증은 위 통합 테스트로 간접 커버되고, 단위 테스트는
  Java 메모리 스코어링만 다룬다(DB 함수라 순수 단위 테스트로 검증하기 어렵다).

### 발견된 이슈: EmotionMatch가 테스트 환경에서 검증하기 어렵다

`EmotionSimilarityUtil`/`HomeRecommendScoreService#emotionMatchExpression`은 감정 ID가 정확히 1~6이라고
하드코딩하고 있다(`for (long candidateEmotionId = 1; candidateEmotionId <= 6; ...)`). 운영 DB는 시드 데이터
(`V2__seed_master_data.sql`)로 이 값이 보장되지만, 테스트 DB(Testcontainers, `ddl-auto: create-drop`)에서는
`Emotion`을 새로 저장할 때마다 IDENTITY 시퀀스가 발급하는 실제 ID가 테스트 클래스/실행 순서에 따라 1~6을
벗어날 수 있다. 이 경우 `EmotionSimilarityUtil.getSimilarityScore(...)`가 모든 후보에 대해 0을 반환해
EmotionMatch가 항상 0이 되어버려서, 통합 테스트로는 EmotionMatch 자체를 신뢰성 있게 검증할 수 없었다(그래서
위 통합 테스트는 모든 후보에 같은 Emotion을 재사용해 이 항을 동일하게 고정해두고 다른 feature로만 정렬을
검증했다). 이건 이번 리팩터링이 만든 문제가 아니라 원래 `EmotionSimilarityUtil`의 설계(감정 ID를 1~6 정수로
하드코딩)에 있던 기존 취약점이며, 시드 데이터가 아닌 환경(테스트, 혹은 향후 DB 마이그레이션/복원으로 시퀀스가
틀어지는 경우)에서 추천이 조용히 EmotionMatch=0으로 저하될 수 있다는 뜻이라 별도로 짚어둔다.

## 갱신 파이프라인 (이벤트 기반 dirty queue + chunk 처리)

전체 유저를 스캔해서 "누가 바뀌었는지" 추론하는 방식(예: `created_at > 마지막갱신시각` 비교)은 테이블이
커질수록 그 스캔 자체가 부담이 된다. 그래서 쓰기 시점에 "이 유저 갱신 필요"를 명시적으로 표시해두는
큐 방식으로 갔다.

1. 링크 저장이 끝나는 시점(`LinkuCreateService.createUsersLinku`)에 그 유저 ID를
   `user_profile_refresh_queue`에 upsert(스캔 없는 O(1) 쓰기, 중복 upsert는 requested_at만 갱신).
2. `UserProfileRefreshWorker`가 5분 주기로 큐에서 오래 기다린 순으로 최대 200명씩 꺼내 처리하고,
   성공한 유저만 큐에서 삭제한다(실패하면 다음 주기에 재시도).
3. 신규 유저처럼 프로필이 아직 없는 경우 TextMatch/KeywordMatch는 0으로 처리한다(에러 아님).

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

### 알려진 한계 — 페이지네이션 근사치 (커서 페이징 전환 전)

지금은 `offset = page * quotaCount`(novelty), `offset = page * (size - quotaCount)`(normal)로 페이지 번호를
근사해서 오프셋을 계산한다. novelty 풀이 소진되기 전까지는 정확하지만, 어느 페이지에서 novelty 후보가
목표치보다 적게 나와서 normal이 더 채워준 뒤에는(borrow), 다음 페이지의 오프셋 계산이 실제로 소비한 양과
어긋나서 중복/누락이 생길 수 있다. 이를 정확히 하려면 novelty/normal 각각의 소비량을 별도로 추적하는
커서(`{noveltyOffset, normalOffset, noveltyExhausted}`)로 페이징 방식 자체를 바꿔야 하는데, 이건 API 계약
변경(FE 협의 필요)이라 이번 스코프에서는 의도적으로 미루고 `page`/`size` 방식을 그대로 유지했다. 커서 페이징
명세는 `docs/home-recommend-cursor-api-spec.md`에 이미 정리해뒀고, 다음 트랙에서 이 근사치를 대체한다.

## 의도적으로 보류한 것

- **DomainDiversity(#8)** — 가중합에 넣지 않고, DB에서 넉넉히(top 20~30) 뽑은 뒤 애플리케이션 레이어에서
  "같은 도메인 연속 노출 제한" 같은 규칙으로 재정렬하는 post-process 단계로 나중에 추가한다.
- **Collaborative(#9)** — 후보군 생성 자체가 지금은 항상 `WHERE user.id = :userId`로 그 유저 본인의 저장
  링크에 한정돼 있어서, 다른 유저의 데이터를 랭킹에 반영하려면 후보군 생성 로직부터 바꿔야 한다. 이건
  스코어링 튜닝보다 큰 결정(다른 유저가 저장한 링크를 서로에게 노출할지 등 제품 결정 포함)이라 별도 트랙.
- **임베딩(cosine similarity) 기반 semantic 매칭** — pgvector + 임베딩 API 호출이 필요한 무거운 인프라라
  TextMatch를 Postgres FTS + trgm으로 먼저 갔고, 품질이 부족하면 그때 검토한다.
- **SituationMatch(#2) 정확도 개선 — 위치 정보 / 폴더 공유 상대와의 관계** — 지금 SituationMatch는 저장
  당시 유저(또는 AI)가 고른 situation 라벨과 요청 situationId의 직접 일치만 본다. 다음 확장으로 두 가지를
  검토할 수 있다: (1) **위치 정보** — 저장 시점의 GPS/위치 컨텍스트를 함께 기록해두면 "집/직장/이동 중" 같은
  situation 추론의 신뢰도를 높이거나, AI 추론(situationAi=true) 시의 confidence 보정에 쓸 수 있다.
  (2) **폴더 공유 상대와의 관계** — `ShareFolderService`/`InvitationService`로 이미 폴더 공유·초대 기능이
  있으므로, "누구와 공유된 폴더에 저장했는가"(예: 친구와 공유한 폴더 vs 혼자 쓰는 폴더)가 situation의
  또 다른 신호가 될 수 있다(같이 저장한 상대와의 관계 유형에 따라 "친구랑 볼 것" 같은 situation을 보정).
  둘 다 아직 원본 데이터(위치 컬럼, 관계 유형 분류)가 없어서 스코어링 축 추가 전에 데이터 수집/스키마 설계가
  선행돼야 한다 — 지금 스코프에는 포함하지 않고 향후 트랙으로 남긴다. 위치/관계 신호로 situation 정확도를
  높이는 방향은 아래 "참고 자료"의 Anand & Bharadwaj situation-aware 논문에서 다루는 접근(유저의
  social-spatiotemporal context를 situation 추론에 반영)과 같은 맥락이다.

## 참고 자료

- Anand, D., & Bharadwaj, K. K. **"Situation-Aware Approach to Improve Context-based Recommender System."**
  ([arXiv:1303.0481](https://arxiv.org/pdf/1303.0481)) — SituationMatch(#2) 설계 시 참고. 유저의
  social-spatiotemporal context("situation")를 추천 랭킹에 직접 반영하는 접근으로, 지금 SituationMatch가
  "저장 당시 situation 라벨 == 요청 situationId 직접 일치"를 보는 방식과 같은 문제의식(situation을 별도
  context 축으로 명시적으로 다룬다)을 공유한다. 위 SituationMatch 확장 아이디어(위치 정보, 폴더 공유 상대와의
  관계로 situation 정확도를 높이는 것)도 이 논문이 다루는 "situation을 더 풍부한 컨텍스트로 정의"하는
  방향의 연장선이다.
- Che, E., Ceylan, H., McInerney, J., & Kallus, N. (Netflix / Columbia / Cornell). **"Optimization of
  Epsilon-Greedy Exploration."** ([arXiv:2506.03324](https://arxiv.org/pdf/2506.03324)) — novelty quota(안 본
  것 버킷을 몇 %나 뽑을지) 비율을 어떻게 정할지 참고. epsilon-greedy의 고정 탐색률(ε)을 감으로 잡는 대신,
  Bayesian regret을 SGD로 직접 최소화해 탐색률 스케줄을 구하고(Model-Predictive Control로 매 배치마다
  재조정) 실측 결과 constant epsilon-greedy보다 일관되게 더 낫다는 걸 보였다. 이 논문의 프레임을 novelty
  quota에 대응시키면, "안 본 것 quota = exploration, 나머지 가중합 랭킹 = exploitation"이 되고, 두 가지를
  시사한다: (1) quota를 전역 고정값 하나로 못박기보다 유저별 트래픽/반응 패턴에 따라 배치 단위로 조정하는
  쪽이 이론적으로 더 낫고, (2) 최소 탐색률 제약(논문에서 최소 탐색률 0.05처럼 하한을 최적화 문제에 넣는 것)처럼
  "novelty quota는 최소 몇 % 이상 보장" 같은 하한 제약을 두는 방식도 참고할 수 있다. 지금은 이런 최적화까지
  가지 않고 고정 비율(예: 20~30%)로 시작해서 실측 튜닝하는 단계이지만, 나중에 quota를 동적으로 조정하고
  싶어지면 이 논문의 MPC 프레임을 재검토한다.

## 다음 액션 아이템

1. `./gradlew compileJava test` / `spotlessApply`로 이번 구현분(1~6) + 테스트 빌드·실행 검증 — 네트워크 제약
   (Gradle/Maven 저장소 접근 불가)으로 이 세션에서는 못 했다. 새로 작성한 두 테스트 파일도 로컬에서 한 번
   실행해서 실제로 통과하는지 확인이 꼭 필요하다.
2. `pg_trgm` 확장 사용 가능 여부 인프라 쪽(RDS 파라미터 그룹) 확인.
3. 마이그레이션(V12) 적용 후 `UserProfileRefreshWorker`가 실제로 큐를 드레인하는지, `EnableScheduling`이
   걸린 환경에서 동작 확인.
4. "발견된 이슈"(EmotionMatch 하드코딩된 ID 1~6 가정)를 어떻게 할지 결정 — 시드 데이터로 고정할지,
   `Emotion` 개수만큼 동적으로 순회하도록 `EmotionSimilarityUtil`/`emotionMatchExpression`을 고칠지.
5. 가중치(`recommend.home.score.weight.*`) 실측 기반 튜닝 — 지금 값(emotion 0.35 / situation 0.15 /
   engagement 0.15 / popularity 0.1 / text 0.1 / keyword 0.05 / category 0.1)은 시작값일 뿐.
   `confidence.ai-emotion-discount`/`ai-situation-discount`(0.8)도 마찬가지로 실측 전 임의값이다.
6. 이후: DomainDiversity 재정렬, Collaborative 축 별도 설계, day-of-week/time-of-day 실시간 신호(저비용·가설
   검증 필요) 및 그 다음 단계로서의 날씨 신호(비동기 캐싱 인프라 + 콘텐츠 태그 체계 선행 필요) 검토.
7. **novelty quota 커서 페이징 전환** — 지금은 page/size 기반 오프셋 근사치를 쓰고 있어서(위 "알려진 한계"
   참고) novelty 풀이 소진된 이후 페이지에서 중복/누락 가능성이 있다. `docs/home-recommend-cursor-api-spec.md`
   명세대로 `{noveltyOffset, normalOffset, noveltyExhausted}` 커서 기반으로 전환 필요 — FE 협의 후 진행.
8. novelty `recencyThresholdDays`(현재 14)/`quotaRatio`(현재 0.3) 실측 기반 튜닝 — 다른 가중치 상수들과
   마찬가지로 시작값일 뿐이다.
