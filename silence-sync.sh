#!/bin/bash
# dev EC2 상태 변화에 맞춰 Grafana의 env=dev 알림을 자동으로 silence/해제.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/silence-sync.env"

GRAFANA_URL="http://localhost:3000"
COMMENT="linku-dev-server-switch-auto"
AUTH_HEADER="Authorization: Bearer $GRAFANA_SA_TOKEN"

state="${1:?state(stopped|running) 인자가 필요합니다}"

existing_id=$(curl -sf -H "$AUTH_HEADER" "$GRAFANA_URL/api/alertmanager/grafana/api/v2/silences" \
  | jq -r --arg c "$COMMENT" '[.[] | select(.comment == $c and .status.state != "expired")][0].id // empty')

if [ "$state" = "stopped" ]; then
  now=$(date -u +%Y-%m-%dT%H:%M:%S.000Z)
  ends="9999-12-31T23:59:59.000Z"
  id_field=""
  if [ -n "$existing_id" ]; then
    id_field=",\"id\":\"$existing_id\""
  fi

  curl -sf -X POST -H "$AUTH_HEADER" -H "Content-Type: application/json" \
    "$GRAFANA_URL/api/alertmanager/grafana/api/v2/silences" \
    -d "{\"matchers\":[{\"name\":\"env\",\"value\":\"dev\",\"isRegex\":false}],\"startsAt\":\"$now\",\"endsAt\":\"$ends\",\"createdBy\":\"linku-server-switch\",\"comment\":\"$COMMENT\"$id_field}" \
    > /dev/null
  echo "silence created (state=$state)"

elif [ "$state" = "running" ]; then
  if [ -n "$existing_id" ]; then
    curl -sf -X DELETE -H "$AUTH_HEADER" "$GRAFANA_URL/api/alertmanager/grafana/api/v2/silence/$existing_id" > /dev/null
    echo "silence expired (state=running)"
  fi
fi
