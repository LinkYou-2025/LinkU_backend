# Keyword 테이블 분리 — 영향 범위 분석

## Gemini가 키워드를 반환하는 현재 흐름

### 경로 1: 링크 생성 시 (`LinkuCreateService`)

```
GeminiLinkuService.analyzeByUrl()
  → LinkuResultDTO(categoryId, keywords: String)    ← "#키워드1, #키워드2" 형식
  → aiKeywords 변수로 받음 (String)
  → createAiArticleIfNeeded(linku, category, emotion, aiKeywords)
  → AiArticle.keyword (TEXT) 에 String 통째로 저장
```

### 경로 2: AI 요약 시 (`AiArticleService.saveAiArticle`)

```
GeminiArticleService.analyzeByUrl()
  → AiArticleResultDTO(title, summary, situationId, emotionId, keywords: String)
  → AiArticleConverter.toEntity(result, ...) → AiArticle.keyword = result.keywords() 저장
  → 단, 기존 AiArticle 업데이트 시 keyword는 업데이트 안 함 (기존 버그)
```

### Gemini 프롬프트 형식 (`LinkSummaryPrompt`)

Gemini가 `"keywords": "#키워드1, #키워드2, ..."` 형식으로 응답하도록 지시함.
→ 파싱 규칙: `#` 제거 후 `,` 기준 split + trim

---

## Keyword를 현재 읽는 곳 (전부 `AiArticle.keyword` String)

| 파일 | 위치 | 용도 |
|---|---|---|
| `LinkuCreateService` | :87, :103 | 기존 링크면 AiArticle에서 읽어서 응답에 포함 |
| `LinkuCreateService` | :120 | `toLinkuResultDTO(..., aiKeywords, ...)` 응답 DTO |
| `LinkuService` | :105 | 링크 상세 조회 응답 |
| `AiArticleService` | :195 | 마이페이지 AI 링크 목록 |
| `FolderServiceImpl` | :346 | 폴더 내 링크 목록 |
| `AiArticleConverter` | :61 | AiArticle → DTO 변환 시 |

---

## 변경 필요 파일 목록

### 저장 로직 (keyword 쓰기)

**`LinkuCreateService`**
- `createAiArticleIfNeeded(linku, category, emotion, aiKeywords)` 메서드:
  - `AiArticle.keyword` 저장 제거
  - keyword String을 파싱 → `Keyword` 테이블에 upsert → `LinkuKeyword` 매핑 저장으로 교체
- `createLinku` 내 `aiKeywords` String 변수:
  - Gemini 결과를 String으로 들고 다니다가 응답 DTO에 넣는 흐름 전체 수정 필요
  - 저장은 `LinkuKeyword`로, 응답은 `Linku.linkuKeywords`에서 읽도록 변경

**`AiArticleConverter`**
- `toEntity(result, ...)`: `.keyword(result.keywords())` 제거 → keyword 저장은 Service 책임으로 이전
- `toEntityKeywordOnly(keyword, ...)`: `.keyword(keyword)` 제거 + 메서드 자체 단순화

**`AiArticleService.saveAiArticle`**
- `AiArticleConverter.toEntity` 호출 후 별도로 keyword 파싱 + `Keyword`/`LinkuKeyword` 저장 추가
- 기존 AiArticle 업데이트 시 keyword도 함께 갱신하도록 수정 (현재 버그 수정)

---

### 조회 로직 (keyword 읽기)

**`LinkuService.detailGetLinku` :105**
```java
// 현재
keyword = aiArticle.getKeyword();

// 변경 후
keyword = linku.getLinkuKeywords().stream()
    .map(lk -> lk.getKeyword().getName())
    .collect(Collectors.joining(", "));
```

**`AiArticleService.getMyAiArticlesByCategory` :195**
```java
// 현재
.keyword(a != null ? a.getKeyword() : "")

// 변경 후
.keyword(l.getLinkuKeywords().stream()
    .map(lk -> lk.getKeyword().getName())
    .collect(Collectors.joining(", ")))
```

**`FolderServiceImpl` :344-347**
```java
// 현재
dto.setKeyword(link.getAiArticle() != null ? link.getAiArticle().getKeyword() : null);

// 변경 후
dto.setKeyword(link.getLinkuKeywords().stream()
    .map(lk -> lk.getKeyword().getName())
    .collect(Collectors.joining(", ")));
```

**`AiArticleConverter.toDto` :61**
- `entity.getKeyword()` 제거
- 호출부에서 keyword를 별도로 전달하거나, Linku에서 직접 읽도록 변경

---

### DTO 변경

**`AiArticleResponsetDTO.AiArticleResultDTO`**
- `keyword: String` → `List<String>` 또는 join String 유지 중 결정 필요

**`LinkuResponseDTO.LinkuResultDTO`**
- `keyword: String` → `List<String>` 또는 join String 유지 중 결정 필요

**`LinkuSummaryDTO`** (`FolderServiceImpl` 에서 사용)
- `keyword: String` → 동일하게 결정 필요

---

### Repository 추가 필요

| Repository | 역할 |
|---|---|
| `KeywordRepository` | `name` 기준 upsert (`findByName` + save) |
| `LinkuKeywordRepository` | `linkuId` 기준 저장 / 삭제 / 조회 |

---

### QueryDSL Q클래스 재생성 필요

엔티티 변경으로 인해 빌드 시 자동 재생성되지만, 아래 필드가 바뀌었으므로 기존 Q클래스를 참조하는 쿼리가 컴파일 에러남.

| Q클래스 | 변경 내용 |
|---|---|
| `QAiArticle` | `keyword`, `title`, `aiFeelingId`, `aiCategoryId`, `situation` 필드 제거됨 |
| `QUsersLinku` | `situation`, `title` 필드 추가됨 |
| `QLinku` | `linkuKeywords` 컬렉션 추가됨 |

→ `AiArticleRepositoryImpl`에서 `aiArticle.title`, `aiArticle.keyword` 참조 부분이 컴파일 에러 발생.

---

### `AiArticleRepositoryImpl` 체크 기준 변경

```java
// 현재: title 기반 (AiArticle.title 제거됨 → 컴파일 에러)
.and(aiArticle.title.isNotNull())
.and(aiArticle.title.isNotEmpty())

// 변경 후: summary 기반
.and(aiArticle.summary.isNotNull())
.and(aiArticle.summary.isNotEmpty())
```

`existsAiArticleByLinkuId`, `existsAiArticleByLinkuIds` 두 메서드 모두 해당.

---

## 요약

| # | 파일 | 변경 이유 |
|---|---|---|
| 1 | `LinkuCreateService` | keyword String → `LinkuKeyword` 저장으로 교체 |
| 2 | `AiArticleConverter` | `toEntity`, `toEntityKeywordOnly`에서 keyword 저장 제거 |
| 3 | `AiArticleService` | keyword 저장/갱신 로직 추가; 업데이트 시 keyword 재저장 (버그 수정) |
| 4 | `LinkuService` | `aiArticle.getKeyword()` → `linku.getLinkuKeywords()` join |
| 5 | `AiArticleService.getMyAiArticlesByCategory` | 동일 |
| 6 | `FolderServiceImpl` | `link.getAiArticle().getKeyword()` → `link.getLinkuKeywords()` join |
| 7 | `AiArticleConverter.toDto` | keyword 파라미터 방식 변경 |
| 8 | `AiArticleResponsetDTO`, `LinkuResponseDTO`, `LinkuSummaryDTO` | keyword 타입 결정 후 수정 |
| 9 | `KeywordRepository`, `LinkuKeywordRepository` | 신규 생성 |
| 10 | `AiArticleRepositoryImpl` | `aiArticle.title` → `aiArticle.summary` 기반 체크로 변경 |

> **Gemini 프롬프트 자체(`LinkSummaryPrompt`, `CategoryClassifyPrompt`)는 변경 불필요.**
> Gemini가 반환하는 String 형식(`#키워드1, #키워드2`)을 파싱해서 테이블에 저장하는 로직만 추가하면 됨.

---

## infra 레이어 추가 영향 범위

### 두 가지 AI 분석 경로 구조

```
경로 1: 링크 생성 (카테고리 분류 + keyword 추출)
  AiLinkuAnalyzer (interface)
    └─ GeminiLinkuService (impl)
         └─ CategoryClassifyPrompt → Gemini → LinkuResultDTO(categoryId, keywords)

경로 2: AI 요약 (summary + keyword 재추출)
  AiArticleAnalyzer (interface)
    └─ GeminiArticleService (impl)
         └─ LinkSummaryPrompt → Gemini → AiArticleResultDTO(title, summary, situationId, emotionId, keywords)
```

---

### 경로 1 — `AiLinkuAnalyzer` / `GeminiLinkuService`

**변경 필요 없음.**
- `CategoryClassifyPrompt`: `categoryId` + `keywords` 반환 → 형식 유지
- `LinkuResultDTO(categoryId, keywords)`: 필드 유지
- `AiLinkuAnalyzer` 인터페이스: 변경 불필요

**변경 필요한 곳은 호출부(`LinkuCreateService`):**
- Gemini가 반환한 `keywords` String을 `AiArticle.keyword`에 넣던 것을 → 파싱해서 `Keyword` + `LinkuKeyword`에 저장하는 것으로 교체

---

### 경로 2 — `AiArticleAnalyzer` / `GeminiArticleService`

이 경로가 영향이 큼. 현재 `AiArticleResultDTO`가 반환하는 5개 필드 중 엔티티 변경으로 3개가 저장처를 잃음.

| `AiArticleResultDTO` 필드 | 기존 저장처 | 변경 후 |
|---|---|---|
| `title` | `AiArticle.title` (제거됨) | ✅ 확정: 링크 생성 시 `UsersLinku.title`에 저장 (사용자 입력 없을 때) |
| `summary` | `AiArticle.summary` | 유지 — AI 요약 전용 |
| `situationId` | `AiArticle.situation` (제거됨) | ✅ 확정: 링크 생성 시 `UsersLinku.situation`에 저장 (사용자 입력 없을 때) |
| `emotionId` | `AiArticle.aiFeelingId` (제거됨) | ✅ 확정: 링크 생성 시 `UsersLinku.emotion`에 저장 (사용자 입력 없을 때) |
| `keywords` | `AiArticle.keyword` (제거됨) | ✅ 확정: `Keyword` + `LinkuKeyword`로 저장 |

→ `AiArticleResultDTO`는 AI 요약 전용이므로 `summary`만 남기고 나머지 제거.
→ `title`/`situationId`/`emotionId`/`keywords`는 **링크 생성 프롬프트(`GeminiLinkuService`)** 로 이동.

#### `AiArticleAnalyzer` 인터페이스 변경 — summary 전용으로 단순화

```java
// 현재: situation/emotion 목록 받아서 situationId/emotionId 반환 → AiArticle에 저장
AiArticleResultDTO analyzeByUrl(String url, List<Situation> situations, List<Emotion> emotions);

// 변경 후: summary만 생성
// title/situation/emotion은 링크 생성 시점 프롬프트로 이전됨
AiArticleResultDTO analyzeByUrl(String url);

// AiArticleResultDTO 변경
record AiArticleResultDTO(String summary)  // summary만 남음
```

---

### ✅ 설계 확정 — situation/emotion/title 처리 방식

확정된 내용:
> - `title`: 사용자 입력 우선, 없으면 Gemini가 생성 → **`UsersLinku.title`에 저장**
> - `situation`: 사용자 입력 우선, 없으면 Gemini가 DB 목록 중 선택 → **`UsersLinku.situation`에 저장**
> - `emotion`: 사용자 입력 우선, 없으면 Gemini가 DB 목록 중 선택 → **`UsersLinku.emotion`에 저장**
> - 홈화면 추천은 `UsersLinku.situation` + `UsersLinku.emotion` 기반으로 동작

| 값 | 저장 위치 | 채우는 주체 |
|---|---|---|
| `title` | `UsersLinku.title` | 사용자 입력 OR Gemini 생성 |
| `situation` | `UsersLinku.situation` | 사용자 입력 OR Gemini가 DB 목록 중 선택 |
| `emotion` | `UsersLinku.emotion` | 사용자 입력 OR Gemini가 DB 목록 중 선택 |
| `summary` | `AiArticle.summary` | AI 요약 기능 실행 시 생성 |
| `imgUrl` | `AiArticle.imgUrl` | AI 요약 기능 실행 시 크롤링 |
| `keywords` | `Keyword` + `LinkuKeyword` | 링크 생성 시 Gemini 분석 결과 |

→ **`AiArticle`에서 `situation`, `aiFeelingId` 제거는 올바른 결정. 복구 불필요.**
→ `AiArticle`은 `summary` + `imgUrl`만 담는 순수 AI 요약 결과 테이블이 됨.

---

### 링크 생성 흐름 변경 — Gemini 호출 전략

**현재 흐름:**
```
LinkuCreateService.createLinku
  → GeminiLinkuService (CategoryClassifyPrompt)
       → categoryId + keywords
  → UsersLinku 생성 (emotion은 사용자 입력, 없으면 default)
```

**변경 후 흐름:**
```
LinkuCreateService.createLinku
  → GeminiLinkuService (CategoryClassifyPrompt)
       → categoryId + keywords

  → 사용자가 title/situationId/emotionId를 입력했는지 확인
     ↳ 하나라도 없으면 → Gemini 추가 호출 필요
          → title(없을 때) + situationId(없을 때) + emotionId(없을 때) 생성
               DB의 situation 목록, emotion 목록을 프롬프트에 포함해야 함

  → UsersLinku 생성
       title    = 사용자 입력 OR Gemini 생성
       situation = 사용자 입력 OR Gemini 선택
       emotion  = 사용자 입력 OR Gemini 선택
```

**문제 — 두 번의 Gemini 호출:**
현재 `GeminiLinkuService`가 category/keywords를 가져오고,
title/situation/emotion을 위해 `GeminiArticleService`를 추가로 호출하면
**링크 생성 시 Gemini를 2번 호출**하게 됨 (응답 시간 증가).

**권장 해결책 — 링크 생성 전용 프롬프트 통합:**
```
새 프롬프트 or CategoryClassifyPrompt 확장
  → categoryId + keywords + title + situationId + emotionId 한 번에 반환
  → situation/emotion 목록을 프롬프트에 포함

출력 예시:
{
  "categoryId": 2,
  "keywords": "#키워드1, #키워드2",
  "title": "...",
  "situationId": 3,
  "emotionId": 1
}
```

이렇게 하면 링크 생성 시 Gemini 호출 1번으로 모두 처리 가능.

---

### infra 영향 범위 최종 정리

| 파일 | 변경 필요 여부 | 내용 |
|---|---|---|
| `AiLinkuAnalyzer` (interface) | ✅ 변경 필요 | 반환 타입 확장 (title, situationId, emotionId 추가) or 새 인터페이스 분리 |
| `GeminiLinkuService` | ✅ 변경 필요 | 프롬프트 통합 — categoryId+keywords+title+situationId+emotionId 반환 |
| `CategoryClassifyPrompt` | ✅ 변경 필요 | situation/emotion 목록 추가, 출력 항목 확장 |
| `LinkuResultDTO` | ✅ 변경 필요 | `title`, `situationId`, `emotionId` 필드 추가 |
| `AiArticleAnalyzer` (interface) | ✅ 변경 필요 | summary 전용으로 단순화, 파라미터(situations, emotions) 제거 |
| `GeminiArticleService` | ✅ 변경 필요 | summary만 반환, situation/emotion 목록 프롬프트 제거 |
| `LinkSummaryPrompt` | ✅ 변경 필요 | summary만 요청하도록 단순화 |
| `AiArticleResultDTO` | ✅ 변경 필요 | `summary`만 남기고 나머지 제거 |
