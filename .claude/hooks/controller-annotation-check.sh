#!/bin/bash
# PostToolUse(Edit|Write) 훅: Controller/Api 파일에 @Tag, @Operation 누락 경고.
# 차단(exit 2)하지 않고 경고만 출력 — 문서 품질은 리뷰 단계에서 확인.

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

CONTENT=$(cat "$FILE_PATH" 2>/dev/null)

# @RestController 또는 @RequestMapping이 없으면 검사 불필요
if ! echo "$CONTENT" | grep -q '@RestController\|@RequestMapping'; then
    exit 0
fi

WARNINGS=()

# @Tag 누락 검사
if ! echo "$CONTENT" | grep -q '@Tag'; then
    WARNINGS+=("@Tag(name = \"...\", description = \"...\") 누락")
fi

# @Operation 누락 검사 (최소 1개 이상 있어야 함)
if ! echo "$CONTENT" | grep -q '@Operation'; then
    WARNINGS+=("@Operation(summary = \"...\") 누락 — 모든 엔드포인트에 필요")
fi

if [ ${#WARNINGS[@]} -gt 0 ]; then
    echo "⚠️  [controller-annotation-check] $(basename "$FILE_PATH")" >&2
    for w in "${WARNINGS[@]}"; do
        echo "   - $w" >&2
    done
fi

exit 0
