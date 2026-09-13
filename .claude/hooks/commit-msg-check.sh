#!/bin/bash
# PreToolUse(Bash) 훅: git commit 명령의 커밋 메시지를 Conventional Commits 형식으로 검증.
# 형식 위반 시 exit 2로 차단.
#
# 차단 이유: 커밋 히스토리 일관성 유지 + CI 자동화 파싱 신뢰도

TOOL_INPUT=$(cat)

COMMAND=$(echo "$TOOL_INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except:
    print('')
" 2>/dev/null)

# git commit 명령이 아니면 통과
if ! echo "$COMMAND" | grep -q 'git commit'; then
    exit 0
fi

# -m 플래그로 전달된 메시지 추출
MSG=$(echo "$COMMAND" | python3 -c "
import sys, re
cmd = sys.stdin.read()
m = re.search(r\"git commit.*?-m ['\\\"](.+?)['\\\"]\"  , cmd, re.DOTALL)
if m:
    print(m.group(1).strip())
" 2>/dev/null)

# 메시지를 추출할 수 없으면 통과 (heredoc, --file 등)
[ -z "$MSG" ] && exit 0

# Conventional Commits 형식 검증
# 허용 패턴: type(scope): description 또는 type: description
CONVENTIONAL_PATTERN='^(feat|fix|refactor|test|docs|perf|chore|style|build|ci|revert)(\([a-zA-Z0-9._-]+\))?: .+'
if ! echo "$MSG" | grep -Eq "$CONVENTIONAL_PATTERN"; then
    echo "❌ [commit-msg-check] Conventional Commits 형식이 아닙니다." >&2
    echo "   형식: type(scope): description" >&2
    echo "   예시: feat(auth): Apple OAuth2 로그인 추가" >&2
    echo "   허용 타입: feat fix refactor test docs perf chore style build ci revert" >&2
    echo "" >&2
    echo "   입력된 메시지: $MSG" >&2
    exit 2
fi

exit 0
