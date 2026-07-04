#!/bin/bash
# PostToolUse(Edit|Write) 훅: @Value 필드 직접 주입 감지 및 경고.
# @ConfigurationProperties 사용을 권장한다.
# 경고 수준 — 차단하지 않음 (@Value가 완전히 금지된 건 아니고, 설정 클래스에서는 허용).

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

# config/ 패키지의 Properties 클래스는 @Value 허용
if echo "$FILE_PATH" | grep -q 'Properties\.java\|config/properties'; then
    exit 0
fi

CONTENT=$(cat "$FILE_PATH" 2>/dev/null)

# @Value("${...}") 패턴 감지
if echo "$CONTENT" | grep -Eq '@Value\("\$\{'; then
    echo "⚠️  [value-injection-check] $(basename "$FILE_PATH")" >&2
    echo "   @Value 필드 직접 주입이 감지되었습니다." >&2
    echo "   @ConfigurationProperties 클래스로 분리하는 것을 권장합니다." >&2
    echo "   참고: config/properties/ 디렉토리의 기존 Properties 클래스를 확인하세요." >&2
fi

exit 0
