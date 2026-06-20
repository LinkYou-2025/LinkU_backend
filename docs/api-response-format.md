# LinkU API Response Format

## 1. 공통 응답 포맷

LinkU API는 아래 공통 응답 포맷을 사용한다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {}
}
```

현재 서버 기준 응답 필드는 아래와 같다.

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| `isSuccess` | `Boolean` | 요청 성공 여부 |
| `code` | `String` | 서버 내부 응답 코드 |
| `message` | `String` | 응답 메시지 |
| `timestamp` | `LocalDateTime` | 응답 시각 |
| `result` | `Object` | 실제 응답 데이터 |

## 2. `result` 정책

- `result`는 항상 응답에 포함한다.
- 조회/생성/수정 등 실제 데이터가 있으면 해당 DTO, 배열, 단순 값을 그대로 담는다.
- 반환할 payload가 없는 성공 응답은 `result: {}` 형태로 내려간다.
- 실패 응답도 현재 공통 포맷을 따르며, 필요 시 `result`에 추가 데이터가 들어갈 수 있다.

예시:

성공 + 데이터 있음

```json
{
  "isSuccess": true,
  "code": "USERS2001",
  "message": "사용자 정보 조회에 성공했습니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {
    "id": 1,
    "nickname": "linku"
  }
}
```

성공 + payload 없음

```json
{
  "isSuccess": true,
  "code": "AUTH2003",
  "message": "인증 코드가 전송되었습니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {}
}
```

실패

```json
{
  "isSuccess": false,
  "code": "COMMON400",
  "message": "잘못된 요청입니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {}
}
```

## 3. 성공 응답 기본 포맷

### 3.1 기본 성공 코드

| 코드 | 이름 | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `COMMON200` | `_OK` | `200 (OK)` | 성공입니다. |
| `COMMON201` | `_CREATED` | `201 (CREATED)` | 성공적으로 생성(저장)되었습니다. |

### 3.2 성공 응답 예시

기본 성공 응답

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {
    "data": "example"
  }
}
```

생성 성공 응답

```json
{
  "isSuccess": true,
  "code": "COMMON201",
  "message": "성공적으로 생성(저장)되었습니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {}
}
```

## 4. 에러 응답 기본 포맷

```json
{
  "isSuccess": false,
  "code": "COMMON500",
  "message": "서버 에러, 관리자에게 문의 바랍니다.",
  "timestamp": "2026-05-15T02:30:00",
  "result": {}
}
```

## 5. 공통 에러 코드

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `COMMON400` | `_BAD_REQUEST` | `400 (BAD_REQUEST)` | 잘못된 요청입니다. |
| `COMMON401` | `_UNAUTHORIZED` | `401 (UNAUTHORIZED)` | 인증이 필요합니다. |
| `COMMON403` | `_FORBIDDEN` | `403 (FORBIDDEN)` | 금지된 요청입니다. |
| `COMMON429` | `_TOO_MANY_REQUESTS` | `429 (TOO_MANY_REQUESTS)` | 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. |
| `COMMON500` | `_INTERNAL_SERVER_ERROR` | `500 (INTERNAL_SERVER_ERROR)` | 서버 에러, 관리자에게 문의 바랍니다. |

## 6. S3 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `S34001` | `_S3_INVALID_FILE` | `400 (BAD_REQUEST)` | 유효하지 않은 파일입니다. |
| `S34002` | `_S3_INVALID_IMAGE` | `400 (BAD_REQUEST)` | 이미지 파일만 업로드할 수 있습니다. |
| `S34003` | `_S3_INVALID_URL` | `400 (BAD_REQUEST)` | 유효하지 않은 S3 URL입니다. |
| `S34004` | `_S3_FILE_EMPTY` | `400 (BAD_REQUEST)` | 업로드할 파일이 없습니다. |
| `S34005` | `_S3_EXTRACT_URL_FAILED` | `400 (BAD_REQUEST)` | URL에서 파일명을 추출할 수 없습니다. |
| `S3404` | `_S3_FILE_NOT_FOUND` | `404 (NOT_FOUND)` | S3 파일을 찾을 수 없습니다. |
| `S35001` | `_S3_UPLOAD_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | S3 파일 업로드에 실패했습니다. |
| `S35002` | `_S3_DELETE_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | S3 파일 삭제에 실패했습니다. |

## 7. OAuth/회원 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `AUTH4001` | `UNAUTHORIZED` | `401 (UNAUTHORIZED)` | 인증이 필요합니다. |
| `AUTH4011` | `INVALID_TOKEN` | `401 (UNAUTHORIZED)` | 잘못된 토큰입니다. |
| `AUTH4002` | `EXPIRED_TOKEN` | `400 (BAD_REQUEST)` | 만료된 토큰입니다. |
| `AUTH4003` | `PERMISSION_DENIED` | `403 (FORBIDDEN)` | 권한이 없습니다. |
| `OAUTH4003` | `_SOCIAL_EMAIL_REQUIRED` | `400 (BAD_REQUEST)` | 소셜 로그인에 이메일이 필요합니다. |
| `OAUTH4004` | `_SOCIAL_UNSUPPORTED_PROVIDER` | `400 (BAD_REQUEST)` | 지원하지 않는 소셜 제공자입니다. |
| `OAUTH4008` | `_INVALID_ID_TOKEN` | `400 (BAD_REQUEST)` | 유효하지 않거나 만료된 ID 토큰 |
| `OAUTH4009` | `_SOCIAL_EXTERNAL_ID_REQUIRED` | `400 (BAD_REQUEST)` | 소셜 계정 ID가 필요합니다. |
| `OAUTH4010` | `_INVALID_EMAIL_FORMAT` | `400 (BAD_REQUEST)` | 올바른 이메일 형식이 아닙니다. |
| `OAUTH5001` | `_AUTH_ACCOUNT_SAVE_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | 소셜 계정 연결에 실패했습니다. |
| `USERS4001` | `_ALREADY_ACTIVE_USER` | `400 (BAD_REQUEST)` | 이미 해당 이메일로 user가 존재합니다. 새로운 회원정보를 만들고 싶다면 탈퇴해주세요. |
| `USERS4002` | `_INVALID_GENDER` | `400 (BAD_REQUEST)` | 성별을 올바르게 선택해야합니다.(MALE: 1, FEMALE: 2) |
| `USERS4004` | `_EXPIRED_VERIFICATION_CODE` | `400 (BAD_REQUEST)` | 인증 코드가 만료되었습니다. |
| `USERS4005` | `_INVALID_PASSWORD` | `400 (BAD_REQUEST)` | 잘못된 비밀번호입니다. |
| `USERS4006` | `_PASSWORD_MISMATCH` | `400 (BAD_REQUEST)` | 비밀번호와 비밀번호 확인이 일치하지 않습니다. |
| `USERS4011` | `_VERIFICATION_FAILED` | `401 (UNAUTHORIZED)` | 인증 코드 검증 실패 |
| `USERS4012` | `_LOGIN_FAILED` | `401 (UNAUTHORIZED)` | 이메일 주소 또는 비밀번호를 다시 확인하세요. |
| `USERS4014` | `_SOCIAL_ACCOUNT_ONLY` | `401 (UNAUTHORIZED)` | 소셜 전용 계정입니다. 소셜 로그인을 이용하세요. |
| `USERS4041` | `_USER_NOT_FOUND` | `404 (NOT_FOUND)` | 사용자를 찾을 수 없습니다. |
| `USERS4042` | `_USER_INACTIVE` | `404 (NOT_FOUND)` | 사용자가 INACTIVE 임시 회원탈퇴 상태입니다. |
| `USERS4091` | `_DUPLICATE_NICKNAME` | `409 (CONFLICT)` | 중복된 닉네임입니다. |
| `USERS4092` | `_DUPLICATE_JOIN_REQUEST` | `409 (CONFLICT)` | 중복된 이메일입니다. |
| `USERS5001` | `_SEND_MAIL_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | 인증 코드 전송 실패 |
| `TERMS4001` | `INVALID_TERMS_TYPE` | `400 (BAD_REQUEST)` | 유효하지 않은 약관 타입입니다. |

## 8. LinkU 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `LINKU4001` | `_LINKU_VIDEO_NOT_ALLOWED` | `400 (BAD_REQUEST)` | 영상 링크는 저장할 수 없습니다. |
| `LINKU4002` | `_LINKU_INVALID_URL` | `400 (BAD_REQUEST)` | 유효하지 않은 링크입니다. |
| `LINKU4003` | `_RECOMMEND_LINKU_NOT_ENOUGH_LINKS` | `400 (BAD_REQUEST)` | 추천을 위해 저장된 링크가 3개 이상이어야 합니다. |
| `LINKU4004` | `_RECOMMEND_LINKU_NO_RECOMMENDATION` | `400 (BAD_REQUEST)` | 추천할 만한 링크가 없습니다. |
| `LINKU4005` | `_RECOMMEND_LINKU_NEW_USER` | `400 (BAD_REQUEST)` | 신규 사용자는 추천 기능을 이용할 수 없습니다. |
| `LINKU404` | `_USER_LINKU_NOT_FOUND` | `404 (NOT_FOUND)` | user_linku 테이블을 찾기 못했습니다. |
| `LINKU4041` | `_LINKU_NOT_FOUND` | `404 (NOT_FOUND)` | 해당 링크 정보를 찾을 수 없습니다. |

## 9. 카테고리/도메인/감정/상황

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `CATEGORY4041` | `_CATEGORY_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 카테고리를 찾을 수 없습니다. |
| `DOMAIN4041` | `_DOMAIN_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 도메인을 찾을 수 없습니다. |
| `EMOTION4041` | `_EMOTION_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 감정을 찾을 수 없습니다. |
| `FOLDER4041` | `_FOLDER_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 폴더를 찾을 수 없습니다. |
| `SITUATION4041` | `_SITUATION_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 상황을 찾을 수 없습니다. |

## 10. 폴더 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `FOLDER4001` | `_FOLDER_INVALID_CURSOR` | `400 (BAD_REQUEST)` | 유효하지 않은 커서 값입니다. |
| `FOLDER4031` | `_FOLDER_CREATE_FORBIDDEN` | `403 (FORBIDDEN)` | 해당하는 폴더를 생성할 권한이 없습니다. |
| `FOLDER4032` | `_FOLDER_UPDATE_FORBIDDEN` | `403 (FORBIDDEN)` | 해당하는 폴더의 수정 권한이 없습니다. |
| `FOLDER4033` | `_FOLDER_DELETE_FORBIDDEN` | `403 (FORBIDDEN)` | 해당하는 폴더의 삭제 권한이 없습니다. |
| `FOLDER4034` | `_FOLDER_ACCESS_FORBIDDEN` | `403 (FORBIDDEN)` | 해당 폴더에 접근 권한이 없습니다. |
| `FOLDER4041` | `_FOLDER_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 폴더를 찾을 수 없습니다. |
| `FOLDER4042` | `_FOLDER_PARENT_NOT_FOUND` | `404 (NOT_FOUND)` | 폴더의 부모 폴더가 없습니다. |
| `FOLDER4043` | `_FOLDER_CATEGORY_NOT_FOUND` | `404 (NOT_FOUND)` | 폴더의 카테고리가 없습니다. |
| `FOLDER4091` | `_FOLDER_CREATE_DUPLICATE` | `409 (CONFLICT)` | 중복된 폴더명입니다. |
| `FOLDER4092` | `_FOLDER_NAME_CONFLICT` | `409 (CONFLICT)` | 카테고리명과 동일한 폴더명은 사용할 수 없습니다. |
| `FOLDER5001` | `_FOLDER_OWNER_NOT_FOUND` | `500 (INTERNAL_SERVER_ERROR)` | 폴더의 소유자 정보를 찾을 수 없습니다. |
| `FOLDER_BOOKMARK404` | `_FOLDER_BOOKMARK_NOT_FOUND` | `404 (NOT_FOUND)` | 해당 유저의 북마크 정보가 존재하지 않습니다. |
| `SHAREFOLDER4001` | `_INVALID_PERMISSION_TYPE` | `400 (BAD_REQUEST)` | 유효하지 않은 권한 타입입니다. |
| `SHAREFOLDER4031` | `_FOLDER_PERMISSION_NOT_ALLOWED` | `403 (FORBIDDEN)` | 폴더 수정 권한을 가지고 있지 않습니다. |
| `SHAREFOLDER4032` | `_FOLDER_OWNER_UPDATE_NOT_ALLOWED` | `403 (FORBIDDEN)` | 폴더 주인의 권한은 수정할 수 없습니다. |
| `SHAREFOLDER4041` | `_FOLDER_PERMISSION_NOT_FOUND` | `404 (NOT_FOUND)` | 해당 유저의 폴더 권한 정보를 찾을 수 없습니다. |

## 11. 초대 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `INVITATION4031` | `INVITATION_CREATOR_CANNOT_ACCEPT` | `403 (FORBIDDEN)` | 초대 생성자는 자신의 링크로 참여할 수 없습니다. |
| `INVITATION4041` | `INVITATION_NOT_FOUND` | `404 (NOT_FOUND)` | 공유 폴더 토큰을 찾을 수 없습니다. |
| `INVITATION4042` | `INVITATION_EXPIRED` | `404 (NOT_FOUND)` | 공유 폴더 토큰이 유효하지 않습니다. |
| `INVITATION4043` | `INVITATION_LINK_NOT_FOUND` | `404 (NOT_FOUND)` | 공유 폴더 링크가 유효하지 않습니다. |

## 12. AI Article / Gemini 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `AIARTICLE4041` | `_AI_ARTICLE_NOT_FOUND` | `404 (NOT_FOUND)` | 해당하는 AI Article을 찾을 수 없습니다. |
| `AIARTICLE4091` | `_DUPLICATE_AI_ARTICLE` | `409 (CONFLICT)` | 이미 해당 링크로 생성된 AI Article이 존재합니다. |
| `AIARTICLE500` | `_INTERNAL_SERVER_ERROR` | `500 (INTERNAL_SERVER_ERROR)` | AI 요약 처리 중 오류가 발생했습니다. |
| `OPENAI5001` | `_AI_PARSE_ERROR` | `400 (BAD_REQUEST)` | AI 응답 파싱에 실패했습니다. |
| `OPENAI5002` | `_AI_INVALID_RESPONSE` | `500 (INTERNAL_SERVER_ERROR)` | AI 응답이 예상한 형식이 아닙니다. |
| `CRAWLER5001` | `_CONTENT_EXTRACTION_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | 웹페이지 본문 추출에 실패했습니다. |
| `CRAWLER5002` | `_CONTENT_EXTRACTION_PROHIBITED` | `500 (INTERNAL_SERVER_ERROR)` | 크롤링이 금지된 웹사이트입니다. |
| `GEMINI4291` | `GEMINI_TOO_MANY_REQUESTS` | `429 (TOO_MANY_REQUESTS)` | AI 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. |
| `GEMINI5001` | `GEMINI_UNKNOWN_ERROR` | `500 (INTERNAL_SERVER_ERROR)` | AI 처리 중 알 수 없는 오류가 발생했습니다. |
| `GEMINI5002` | `GEMINI_RESPONSE_FORMAT_ERROR` | `500 (INTERNAL_SERVER_ERROR)` | AI 응답 형식이 올바르지 않습니다. |
| `GEMINI5021` | `GEMINI_BAD_REQUEST` | `400 (BAD_REQUEST)` | 잘못된 AI 요청입니다. |
| `GEMINI5021` | `GEMINI_API_ERROR` | `502 (BAD_GATEWAY)` | Gemini API 호출 중 오류가 발생했습니다. |
| `GEMINI5022` | `GEMINI_PARSE_ERROR` | `502 (BAD_GATEWAY)` | AI 응답 JSON 파싱에 실패했습니다. |
| `GEMINI5041` | `GEMINI_TIMEOUT` | `504 (GATEWAY_TIMEOUT)` | Gemini 응답 시간이 초과되었습니다. |

## 13. 알림 관련 에러

| 예외 코드 | 예외 이름(서버에서 사용) | Http 코드 | 메시지 |
| --- | --- | --- | --- |
| `ALARM_NOT_FOUND` | `ALARM_NOT_FOUND` | `404 (NOT_FOUND)` | 알람을 찾을 수 없습니다. |
| `ALARM_PERMISSION_DENIED` | `ALARM_PERMISSION_DENIED` | `401 (UNAUTHORIZED)` | 알람에 대한 권한이 없습니다. |
| `ALARM5001` | `ALARM_TOPIC_SUBSCRIPTION_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | 알림 주제 구독 상태 변경에 실패했습니다. |
| `ALARM5002` | `ALARM_SEND_FAILED` | `500 (INTERNAL_SERVER_ERROR)` | 알림 전송에 실패했습니다. |

## 14. 구현 기준 메모

- 성공 응답 생성: `ApiResponse.onSuccess(...)`
- 실패 응답 생성: `ApiResponse.onFailure(...)`
- payload 없는 성공 응답: `ApiResponse.onSuccess(code)` 사용
- 현재 payload 없는 성공 응답은 `result: {}` 형태로 내려가도록 맞춘 상태
- 기본 성공 코드 사용 시 `SuccessStatus._OK`
- 도메인별 성공 코드는 `AuthSuccessStatus`, `UserSuccessStatus`, `FolderSuccessStatus`, `LinkuSuccessStatus`, `AiArticleSuccessStatus`, `AlarmSuccessStatus`, `CategorySuccessStatus` 등에서 관리
