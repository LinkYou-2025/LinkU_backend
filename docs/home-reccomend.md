# 홈화면 링크 추천 스코어링

추천 점수: scoreBucket

EmotionMatch: 현재 추천하려는 링크의 감정이랑, input으로 넣은 감정 유사도
Situtation: 현재 추천하려는 링크의 상황이랑, input으로 넣은 상황 유사도
PersonalEngagement: 사용자가 어떤 링크를 본 viewcount랑, 오래 안 보거나 안 만든 정도(staleness)를 가지고 계산한 점수
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
| 3 | PersonalEngagement | `UsersLinku.viewCount`, `lastViewedAt`(없으면 `createdAt`) | 실시간 SQL (viewCount 캡 정규화 + staleness(오래될수록 커지는 값)의 평균) | ✅ 구현 완료 |
| 4 | Popularity | `Linku.totalViewCount` | 실시간 SQL (로그 정규화 + 캡) | ✅ 구현 완료 |
| 5 | TextMatch | `Linku.title`, `AiArticle.summary` | 사전계산된 유저 프로필과 Postgres FTS 매칭 (trgm fallback) | ✅ 구현 완료 |
| 6 | KeywordMatch | `LinkuKeyword`/`Keyword` | 사전계산된 유저 키워드 프로필과 스칼라 서브쿼리 매칭 | ✅ 구현 완료 |
| 7 | CategoryMatch | `Linku.category` | 실시간 SQL (situation→category 매핑(`SituationCategoryService`)에 걸리면 1.0, 아니면 0) | ✅ 구현 완료 |

### sql 계산
base CTE: 추천 후보 링크를 가져오고 텍스트 점수도 미리 계산
scored CTE: base CTE에서 가져온 값을 이용해 7중 가중합 계산 , score_bucket 계산
cursor 계산: score_bucket 순으로 정렬, LIMIT 적용

### Textmatch
title_tsv/summary_tsv generated column + GIN 인덱스는 한 번 추가했다가 뺐다 — `@@` 검색 조건 없이
`ts_rank_cd`로 랭킹만 계산하는 지금 쿼리 구조에서는 GIN이 실행계획에 전혀 안 잡혀서(＠＠가 있어야 GIN이
쓰임) 실익 없이 컬럼/인덱스만 늘리는 꼴이었다. 대신 `to_tsvector('simple', title || ' ' || summary)`를
요청마다 즉석 계산하되, base CTE에서 후보 행당 딱 한 번만 계산해 `fts_rank` 컬럼으로 두고 scored
단계에서는 그 값을 읽기만 한다 — 예전엔 이 계산식이 CASE 절 안에서 4번 반복 삽입돼 행마다 4번씩
실행됐는데, 그 중복만 없앤 것으로 충분했다.

## personalEngagement
최근에 보거나, 최근에 수정한 것의 추천을 줄임
PersonalEngagement
= 1/2 × [ min(viewCount, cap) / cap
+ 1 - exp( - days(now - COALESCE(lastViewedAt, createdAt)) / 14 ) ]
