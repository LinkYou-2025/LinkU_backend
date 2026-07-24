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
| 2 | SituationMatch | `UsersLinku.situation`, `Linku.category` | 실시간 SQL (직접 일치 1.0 우선, situation→category 매핑 0.6 보조) | ✅ 구현 완료 |
| 3 | PersonalEngagement | `UsersLinku.viewCount`, `lastViewedAt` | 실시간 SQL (viewCount 캡 정규화 + lastViewedAt 지수감쇠 평균) | ✅ 구현 완료 |
| 4 | Popularity | `Linku.totalViewCount` | 실시간 SQL (로그 정규화 + 캡) | ✅ 구현 완료 |
| 5 | TextMatch | `Linku.title`, `AiArticle.summary` | 사전계산된 유저 프로필과 Postgres FTS 매칭 (trgm fallback) | ✅ 구현 완료 |
| 6 | KeywordMatch | `LinkuKeyword`/`Keyword` | 사전계산된 유저 키워드 프로필과 스칼라 서브쿼리 매칭 | ✅ 구현 완료 |
| 7 | DomainDiversity | `Linku.domain` | 가중합이 아닌 결과 재정렬(post-process) | ⏸ 보류 |
| 8 | Collaborative(교차 유저) | (미정 — 다른 유저의 저장/키워드 데이터) | 미정 | ⏸ 향후 확장 축, 설계 전 |

1~4는 전부 이미 있는 스칼라 컬럼이라 새 테이블 없이 구현됐다. 5~6은 "비교 대상(유저 프로필)"이 필요해서
사전계산 인프라(마이그레이션 + 워커)까지 포함해 구현했다. 7, 8은 이번 스코프에서 의도적으로 제외했다.

## 구현된 것

- `config/properties/RecommendScoreProperties.java` — 가중치(emotion/situation/engagement/popularity/text/keyword) +
  정규화 상수(viewCountCap, recencyHalfLifeDays, popularityViewCountCap, keywordWeightCap) +
  신뢰도 상수(`Confidence.aiEmotionDiscount`/`aiSituationDiscount`).
- `application.yml`의 `recommend.home.score.*` — 시작값일 뿐 실측 튜닝 필요.
- `service/common/HomeRecommendScoreService.java` — Java 메모리용(`score`, `FeatureVector`)과 QueryDSL
  표현식용(`scoreExpression` 등) 두 형태로 동일한 공식을 제공. feature 벡터는 6차원
  (EmotionMatch/SituationMatch/PersonalEngagement/Popularity/TextMatch/KeywordMatch). 감정 유사도 공식은
  여전히 `EmotionSimilarityUtil` 하나가 진실 공급원. EmotionMatch/SituationMatch는 `UsersLinku.emotionAi`/
  `situationAi`(AI 추론 여부)에 따라 신뢰도 감쇠가 추가로 곱해진다(아래 "AI vs 유저 직접 분류 신뢰도 가중치" 참고).
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
situationMatch  = base(직접일치 1.0 / category매핑 0.6)  * (situationAi ? aiSituationDiscount : 1.0)
```

Java 메모리 버전(`emotionMatch`/`situationMatch`)은 `candidateEmotionIsAi`/`candidateSituationIsAi` boolean
파라미터를 추가로 받고, QueryDSL 버전(`emotionMatchExpression`/`situationMatchExpression`)은 정규화된 점수에
`CASE WHEN usersLinku.emotionAi = true THEN :discount ELSE 1.0 END` 형태의 감쇠 factor를 곱해서 처리한다 —
둘 다 별도 컬럼/조인 추가 없이 이미 존재하는 `emotionAi`/`situationAi` 컬럼만 읽는다.

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
  (emotionMatch/situationMatch/personalEngagement/popularity 각각의 경계값 + score() 가중합 검증). Spring
  컨텍스트 없이 `RecommendScoreProperties`를 직접 생성해 순수 POJO로 테스트한다. emotionMatch/situationMatch는
  AI 추론(`candidateEmotionIsAi`/`candidateSituationIsAi` = true) 시 `aiEmotionDiscount`/`aiSituationDiscount`
  (0.8)만큼 정확히 감쇠되는지도 검증한다.
- `src/test/.../repository/UserLinkuRepository/UsersLinkuRepositoryImplTest.java` — `findHomeRecommendCandidates`
  통합 테스트(Testcontainers Postgres). SituationMatch 직접일치>category매핑>매칭없음 순서, situation=null인
  후보가 결과에서 빠지지 않는지(LEFT JOIN 회귀 검증), PersonalEngagement/Popularity가 높을수록 상위로 오는지를
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

## 의도적으로 보류한 것

- **DomainDiversity(#7)** — 가중합에 넣지 않고, DB에서 넉넉히(top 20~30) 뽑은 뒤 애플리케이션 레이어에서
  "같은 도메인 연속 노출 제한" 같은 규칙으로 재정렬하는 post-process 단계로 나중에 추가한다.
- **Collaborative(#8)** — 후보군 생성 자체가 지금은 항상 `WHERE user.id = :userId`로 그 유저 본인의 저장
  링크에 한정돼 있어서, 다른 유저의 데이터를 랭킹에 반영하려면 후보군 생성 로직부터 바꿔야 한다. 이건
  스코어링 튜닝보다 큰 결정(다른 유저가 저장한 링크를 서로에게 노출할지 등 제품 결정 포함)이라 별도 트랙.
- **임베딩(cosine similarity) 기반 semantic 매칭** — pgvector + 임베딩 API 호출이 필요한 무거운 인프라라
  TextMatch를 Postgres FTS + trgm으로 먼저 갔고, 품질이 부족하면 그때 검토한다.

## 다음 액션 아이템

1. `./gradlew compileJava test` / `spotlessApply`로 이번 구현분(1~6) + 테스트 빌드·실행 검증 — 네트워크 제약
   (Gradle/Maven 저장소 접근 불가)으로 이 세션에서는 못 했다. 새로 작성한 두 테스트 파일도 로컬에서 한 번
   실행해서 실제로 통과하는지 확인이 꼭 필요하다.
2. `pg_trgm` 확장 사용 가능 여부 인프라 쪽(RDS 파라미터 그룹) 확인.
3. 마이그레이션(V12) 적용 후 `UserProfileRefreshWorker`가 실제로 큐를 드레인하는지, `EnableScheduling`이
   걸린 환경에서 동작 확인.
4. "발견된 이슈"(EmotionMatch 하드코딩된 ID 1~6 가정)를 어떻게 할지 결정 — 시드 데이터로 고정할지,
   `Emotion` 개수만큼 동적으로 순회하도록 `EmotionSimilarityUtil`/`emotionMatchExpression`을 고칠지.
5. 가중치(`recommend.home.score.weight.*`) 실측 기반 튜닝 — 지금 값(0.35/0.25/0.15/0.1/0.1/0.05)은 시작값일 뿐.
   `confidence.ai-emotion-discount`/`ai-situation-discount`(0.8)도 마찬가지로 실측 전 임의값이다.
6. 이후: DomainDiversity 재정렬, Collaborative 축 별도 설계, day-of-week/time-of-day 실시간 신호(저비용·가설
   검증 필요) 및 그 다음 단계로서의 날씨 신호(비동기 캐싱 인프라 + 콘텐츠 태그 체계 선행 필요) 검토.
