#!/usr/bin/env bash

set -euo pipefail

if [[ -z "${DISCORD_WEBHOOK_URL:-}" ]]; then
  echo "ERROR: DISCORD_WEBHOOK_URL is required." >&2
  exit 1
fi

if [[ -z "${REVIEWER_MAP:-}" ]]; then
  echo "ERROR: REVIEWER_MAP is required." >&2
  exit 1
fi

if [[ -z "${REPO:-}" ]]; then
  echo "ERROR: REPO is required." >&2
  exit 1
fi

if ! jq -e 'type == "object" and all(.[]; type == "string" and test("^[0-9]+$"))' \
  >/dev/null <<<"$REVIEWER_MAP"; then
  echo "ERROR: REVIEWER_MAP must map GitHub logins to Discord numeric IDs." >&2
  exit 1
fi

declare -A discord_ids reviewer_prs reviewer_counts known_reviewers

while IFS=$'\t' read -r login discord_id; do
  [[ -z "$login" ]] && continue
  discord_ids["$login"]="$discord_id"
  reviewer_prs["$login"]=""
  reviewer_counts["$login"]=0
  known_reviewers["$login"]=1
done < <(jq -r 'to_entries[] | [.key, .value] | @tsv' <<<"$REVIEWER_MAP")

pulls_json=$(gh api "repos/$REPO/pulls?state=open&per_page=100" --paginate | jq -s 'add')
now_epoch=$(date -u +%s)

while IFS=$'\t' read -r number created_at html_url reviewers; do
  [[ -z "$number" ]] && continue
  timeline_json=$(gh api "repos/$REPO/issues/$number/timeline" --paginate | jq -s 'add')

  while IFS= read -r reviewer; do
    [[ -z "$reviewer" ]] && continue
    known_reviewers["$reviewer"]=1
    reviewer_counts["$reviewer"]=$(( ${reviewer_counts["$reviewer"]:-0} + 1 ))

    assigned_at=$(jq -r --arg reviewer "$reviewer" '
      [ .[]
        | select(.event == "review_requested")
        | select(.requested_reviewer.login? == $reviewer)
      ]
      | max_by(.created_at) // empty
      | .created_at // empty
    ' <<<"$timeline_json")
    [[ -z "$assigned_at" ]] && assigned_at="$created_at"

    assigned_epoch=$(date -u -d "$assigned_at" +%s)
    elapsed_days=$(( (now_epoch - assigned_epoch) / 86400 ))
    reviewer_prs["$reviewer"]+="${elapsed_days}"$'\t'"${html_url}"$'\n'
  done < <(jq -r '.[]' <<<"$reviewers")
done < <(
  jq -r '
    .[]
    | select(.draft != true)
    | [.number, .created_at, .html_url, ([.requested_reviewers[]?.login] | tojson)]
    | @tsv
  ' <<<"$pulls_json"
)

send_message() {
  local msg="$1"
  local payload
  payload=$(jq -n --arg content "$msg" '{content: $content, allowed_mentions: {parse: ["users"]}}')
  curl -sS -f -H "Content-Type: application/json" -d "$payload" "$DISCORD_WEBHOOK_URL" >/dev/null
}

send_reviewer_block() {
  local block="$1"
  local chunk=""
  local line

  # Preserve reviewer grouping; only a single oversized reviewer block is split.
  while IFS= read -r line || [[ -n "$line" ]]; do
    if (( ${#chunk} > 0 && ${#chunk} + ${#line} + 1 > 1900 )); then
      send_message "$chunk"
      chunk=""
    fi
    if [[ -n "$chunk" ]]; then
      chunk+=$'\n'
    fi
    chunk+="$line"
  done <<<"$block"

  printf '%s' "$chunk"
}

current_message=""
while IFS=$'\t' read -r count login; do
  mention="@${login}"
  if [[ -n "${discord_ids["$login"]:-}" ]]; then
    mention="<@${discord_ids["$login"]}>"
  fi

  if (( count == 0 )); then
    block="${mention}님의 남은 코드 리뷰 개수는 0개 입니다. 🎉 밀린 리뷰 없이 완벽해요, 최고예요!"
  else
    block="${mention}님의 남은 코드 리뷰 개수는 ${count}개 입니다."
    while IFS=$'\t' read -r elapsed_days html_url; do
      [[ -z "$html_url" ]] && continue
      block+=$'\n'"- ${html_url} (할당 후 ${elapsed_days}일 경과)"
    done < <(printf '%s' "${reviewer_prs["$login"]}" | sort -t $'\t' -k1,1nr)
  fi

  if (( ${#block} > 1900 )); then
    [[ -n "$current_message" ]] && send_message "$current_message"
    current_message=$(send_reviewer_block "$block")
  elif (( ${#current_message} == 0 )); then
    current_message="$block"
  elif (( ${#current_message} + ${#block} + 2 <= 1900 )); then
    current_message+=$'\n\n'"$block"
  else
    send_message "$current_message"
    current_message="$block"
  fi
done < <(
  for login in "${!known_reviewers[@]}"; do
    printf '%s\t%s\n' "${reviewer_counts["$login"]:-0}" "$login"
  done | sort -t $'\t' -k1,1nr -k2,2
)

[[ -n "$current_message" ]] && send_message "$current_message"
