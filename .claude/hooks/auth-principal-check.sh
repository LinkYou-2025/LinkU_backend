#!/bin/bash
# PostToolUse(Edit|Write) 훅: @AuthenticationPrincipal 직접 사용 감지 후 차단.
# 반드시 @CurrentUser 커스텀 어노테이션을 사용해야 한다.
#
# 차단 이유: @AuthenticationPrincipal 직접 사용 시 타입 안전성이 떨어지고
#            인증 로직 변경 시 수정 범위가 커짐.

TOOL_INPUT=$(cat)

FILE_PATH=$(echo "$TOOL_INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except:
    print('')
" 2>/dev/null)

[ -z "$FILE_PATH" ] && exit 0
[ ! -f "$FILE_PATH" ] && exit 0

# CurrentUserArgumentResolver 자체는 예외
if echo "$FILE_PATH" | grep -q 'CurrentUserArgumentResolver\|CurrentUser\.java'; then
    exit 0
fi

CONTENT=$(cat "$FILE_PATH" 2>/dev/null)

if echo "$CONTENT" | grep -q '@AuthenticationPrincipal'; then
    echo "❌ [auth-principal-check] $(basename "$FILE_PATH")" >&2
    echo "   @AuthenticationPrincipal 직접 사용은 금지됩니다." >&2
    echo "   @CurrentUser CustomUserDetails userDetails 를 사용하세요." >&2
    echo "" >&2
    echo "   변경 전: @AuthenticationPrincipal CustomUserDetails userDetails" >&2
    echo "   변경 후: @CurrentUser CustomUserDetails userDetails" >&2
    exit 2
fi

exit 0
