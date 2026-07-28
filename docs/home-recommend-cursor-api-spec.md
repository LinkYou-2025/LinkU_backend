# 화면 링크 추천 API — 커서 페이징 전환 명세 (FE 공유용)

## 배경

홈화면 추천(`GET /recommend`)에 "최근에 안 본 링크" 우선 노출(novelty quota) 로직을 추가하면서, 페이징 방식을
기존 `page`(offset 기반)에서 `cursor` 기반으로 바꾼다. novelty 후보군과 일반(가중합 랭킹) 후보군을 각각
별도로 조회해서 한 페이지를 구성하는데, 이 두 후보군은 서로 다른 속도로 소진되기 때문에(novelty 후보가
일반 후보보다 훨씬 적음) 단일 `offset` 값 하나로는 두 후보군의 진행 위치를 동시에 표현할 수 없다. 그래서
서버가 두 후보군의 진행 상태를 인코딩한 커서 문자열을 내려주고, FE는 그 값을 그대로 다음 요청에 전달하는
방식으로 바꾼다.

**FE가 알아야 할 것은 딱 하나다: `page` 파라미터가 없어지고 `cursor` 파라미터가 생긴다. `cursor` 값은
서버가 준 문자열을 그대로 복사해서 다음 요청에 넣기만 하면 되고, 그 안의 내용을 파싱하거나 계산할 필요는
없다.** (마이페이지 "AI 요약 글 보기"의 `cursor` 파라미터와 동일한 사용 패턴이다.)

## 변경 전 (현재)

```
GET /recommend?situationId={situationId}&emotionId={emotionId}&page={page}&size={size}
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| situationId | Long | Y | - | 유저가 홈에서 선택한 상황 ID |
| emotionId | Long | Y | - | 유저가 홈에서 선택한 감정 ID |
| page | int | N | 0 | 0부터 시작하는 페이지 번호 |
| size | int | N | 5 | 페이지당 개수 |

응답: `ApiResponse<List<LinkuSimpleDTO>>` — 배열만 내려주고 다음 페이지 존재 여부는 FE가 `size`만큼
안 채워졌는지로 추측해야 했다.

## 변경 후 (신규)

```
GET /recommend?situationId={situationId}&emotionId={emotionId}&cursor={cursor}&size={size}
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| situationId | Long | Y | - | (변경 없음) |
| emotionId | Long | Y | - | (변경 없음) |
| cursor | String | N | 없음(첫 요청 시 생략) | 이전 응답의 `nextCursor` 값을 그대로 전달. 첫 요청에는 파라미터 자체를 안 보내면 된다 |
| size | int | N | 5 | (변경 없음, 페이지당 개수) |

**`page` 파라미터는 제거된다.** 서버가 커서 안에 "지금까지 novelty/normal 각각 몇 개를 내려줬는지"를
인코딩해서 관리하기 때문에, FE가 페이지 번호를 계산해서 넘기는 방식 자체가 사라진다.

### 응답 형태

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "items": [
      {
        "linkuId": 123,
        "linku": "https://...",
        "emotionId": 1,
        "domain": "...",
        "domainImageUrl": "...",
        "title": "...",
        "linkuImageUrl": "..."
      }
    ],
    "nextCursor": "eyJub3ZlbHR5QnVja2V0Ijo5OCwibm92ZWx0eUxhc3RJZCI6MTIsIm5vcm1hbEJ1Y2tldCI6MjEwLCJub3JtYWxMYXN0SWQiOjg3LCJub3ZlbHR5RXhoYXVzdGVkIjpmYWxzZX0=",
    "hasNext": true
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items | array | 추천 링크 목록 (필드 구성은 기존 `LinkuSimpleDTO`와 동일, 변경 없음) |
| nextCursor | String \| null | 다음 페이지 요청 시 `cursor` 파라미터에 그대로 넣을 값. 마지막 페이지면 `null` |
| hasNext | boolean | 다음 페이지 존재 여부. `false`면 무한스크롤 로딩을 멈추면 됨 |

### 무한스크롤 연동 방법 (FE 구현 가이드)

1. 화면 진입 시 `cursor` 파라미터 없이 첫 요청 (`size`만 지정)
2. 응답의 `hasNext`가 `true`면 스크롤이 바닥에 닿을 때 `nextCursor` 값을 `cursor` 파라미터에 그대로 실어서
   다음 요청
3. 응답의 `hasNext`가 `false`이면 더 요청하지 않고 로딩 UI 종료
4. `cursor` 값은 FE에서 저장만 하고 파싱/가공하지 않는다 (내부 구조가 바뀌어도 FE 코드는 영향 없음)

### 예시 흐름

```
1차 요청: GET /recommend?situationId=19&emotionId=1&size=5
1차 응답: { items: [5개], nextCursor: "eyJub3ZlbHR5QnVja2V0IjoxNTAsIm5vdmVsdHlMYXN0SWQiOjUsIm5vcm1hbEJ1Y2tldCI6bnVsbCwibm9ybWFsTGFzdElkIjpudWxsLCJub3ZlbHR5RXhoYXVzdGVkIjpmYWxzZX0=", hasNext: true }

2차 요청: GET /recommend?situationId=19&emotionId=1&size=5&cursor=eyJub3ZlbHR5QnVja2V0IjoxNTAsIm5vdmVsdHlMYXN0SWQiOjUsIm5vcm1hbEJ1Y2tldCI6bnVsbCwibm9ybWFsTGFzdElkIjpudWxsLCJub3ZlbHR5RXhoYXVzdGVkIjpmYWxzZX0=
2차 응답: { items: [5개], nextCursor: "...", hasNext: true }

...

N차 요청 (novelty 후보 소진 이후): 응답 items는 계속 5개씩 내려오되(normal 후보만으로 채움),
커서 내부적으로 noveltyExhausted=true 상태가 유지된다 — FE 입장에서는 요청/응답 형태가 동일해서 신경 쓸 것 없음

마지막 요청: { items: [3개], nextCursor: null, hasNext: false }
```

## 에러 응답

기존과 동일 (변경 없음): `_SITUATION_NOT_FOUND`, `_EMOTION_NOT_FOUND`, `_RECOMMEND_LINKU_NOT_ENOUGH_LINKS`,
`_RECOMMEND_LINKU_NO_RECOMMENDATION`, `_RECOMMEND_LINKU_NEW_USER`, `_USER_NOT_FOUND`, `_JOB_NOT_SET`.

`cursor` 파라미터 자체는 검증하지 않고, 디코딩 실패 시(예: 잘못된 값을 강제로 넣은 경우) 서버는 첫 페이지로
간주하고 처리한다 (에러를 던지지 않음 — 안전한 폴백).

## 마이그레이션 시 FE 체크리스트

- [ ] 무한스크롤 로직에서 `page` 번호를 직접 증가시키던 코드를 제거하고, 응답의 `nextCursor`를 저장해뒀다가
      다음 요청에 그대로 넘기는 방식으로 변경
- [ ] "다음 페이지 있는지" 판단을 `items.length === size` 같은 추측 대신 `hasNext` 필드로 대체
- [ ] 새로고침/화면 재진입 시 저장해둔 `cursor` 값을 초기화하고 첫 요청부터 다시 시작하는지 확인
- [ ] `cursor` 값을 로그/분석 이벤트 등에 노출하지 않기 (내부 구현 세부사항이라 언제든 포맷이 바뀔 수 있음)

## 구현 메모 (서버)

- 커서는 `{noveltyBucket, noveltyLastId, normalBucket, normalLastId, noveltyExhausted}`를 JSON으로 만든 뒤
  표준 Base64로 인코딩한 불투명 문자열이다 (`com.umc.linkyou.utils.RecommendCursorUtil`). FE가 이 필드들을
  알 필요는 없다 — 그대로 복사만 하면 된다는 원칙은 그대로다.
- OFFSET이 아니라 seek(keyset) 방식이다. `noveltyBucket`/`normalBucket`은 정렬에 쓰인 score를 정수 구간으로
  양자화한 값이고, `noveltyLastId`/`normalLastId`는 그 구간 안에서의 타이브레이크 키(userLinkuId)다. 페이지가
  깊어져도 이전 페이지들을 다시 스캔/스킵하지 않는다.
- 서버는 각 페이지마다 novelty/normal 두 버킷을 "목표 개수 + 1"건씩 조회해 다음 페이지 존재 여부를 판별하고,
  이번 페이지에서 실제로 반환한 마지막 행의 (bucket, id)를 다음 커서로 만든다 (`LinkuRecommendService`).
- novelty 버킷이 소진되면(`noveltyExhausted=true`) 이후 요청부터는 novelty 조회를 생략하고 normal 버킷만으로
  나머지 자리를 채운다.
