#!/bin/bash
# PostToolUse(Edit|Write) 통합 디스패처
# 파일 경로에 따라 관련 검증 훅만 선택적으로 실행.
#
# 차단 신호(exit 2)는 반드시 상위로 전파 — blocking 훅이 무음 실패하지 않도록.

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

HOOKS_DIR="$(dirname "$0")"

FINAL_EXIT=0

run_hook() {
    local hook="$1"
    echo "$TOOL_INPUT" | bash "$HOOKS_DIR/$hook"
    local rc=$?
    if [ "$rc" -eq 2 ]; then
        FINAL_EXIT=2
    elif [ "$rc" -ne 0 ]; then
        echo "⚠️  $hook exited with code $rc (non-blocking)" >&2
    fi
}

case "$FILE_PATH" in
    *Controller.java|*Api.java)
        run_hook controller-annotation-check.sh
        run_hook auth-principal-check.sh
        ;;
    *.java)
        run_hook value-injection-check.sh
        run_hook auth-principal-check.sh
        ;;
esac

exit $FINAL_EXIT
