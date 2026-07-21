---
name: git-conventions
description: Use when creating commits, branches, pull requests, or GitHub issues. Covers Conventional Commits format (Korean preferred), branch naming, PR/issue templates, and merge etiquette. Trigger any time a git/gh command is about to run or when drafting a PR description or branch name.
---

# Git Conventions

## 커밋 메시지

**Conventional Commits** 형식, **한글 우선** (한글/영문 모두 허용, `commit-msg-check.sh` 훅이 형식만 강제).

```
type: #이슈번호 한글 설명
```

### 타입

| 타입 | 사용 시점 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서 작성/수정 |
| `perf` | 성능 개선 |
| `chore` | 빌드/설정/의존성 변경 |
| `style` | 포맷/공백 등 비기능 변경 |

### 예시

```
feat: #12 Apple OAuth2 로그인 구현
fix: #34 폴더에 링크 없을 때 NPE 수정
refactor: #56 알람 상태 업데이트 로직 서비스로 분리
test: #78 마케팅 약관 토글 통합 테스트 추가
chore: #90 spring-boot 3.4.7로 업그레이드
```

### 금지 패턴

```
# scope 사용 금지
feat(auth): 로그인 구현  ❌

# 이슈번호 없음 (이슈가 있는 경우)
feat: 로그인 구현  ❌

# 타입 없음
로그인 구현  ❌

# 너무 모호함
fix: 버그 수정  ❌
```

---

## 브랜치 명명

```
feat/short-description       # 새 기능
fix/short-description        # 버그 수정
refactor/short-description   # 리팩토링
chore/short-description      # 설정/의존성
docs/short-description       # 문서
```

예시:
- `feat/apple-oauth-login`
- `fix/alarm-duplicate-notification`
- `chore/upgrade-spring-boot`

**base 브랜치**: `develop` (main은 배포 브랜치)

---

## PR 작성

PR 제목은 커밋 메시지와 동일한 Conventional Commits 형식으로:

```
feat: #12 Apple OAuth2 로그인 구현
```

PR 본문 구성:
1. **Summary** — 무엇을 왜 변경했는지 (2-4줄)
2. **Changes** — 주요 변경 파일/로직 목록
3. **Test** — 어떻게 테스트했는지
4. **Review Points** — 리뷰어가 집중해야 할 부분 (선택)

---

## Merge 규칙

- `develop` → `main`: 배포 준비 완료 후만
- PR은 최소 1 Approve 후 merge
- Squash merge 권장 (feature 브랜치)
- 충돌 발생 시 base 브랜치를 rebase로 가져온 뒤 해결

---

## GitHub Issues

이슈 제목도 Conventional Commits 형식 권장:

```
feat: 링크 공유 폴더 초대 기능
bug: 알림 중복 발송 문제
```
