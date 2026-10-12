#!/bin/bash
# 다음 릴리즈 버전(vX.Y.Z)을 계산해 stdout으로 출력
set -euo pipefail

latest_tag=$(git tag -l 'v*' | { grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true; } | sort -V | tail -n 1)
if [ -z "$latest_tag" ]; then
  next_tag="v1.0.0"
else
  version=${latest_tag#v}
  major=$(echo "$version" | cut -d. -f1)
  minor=$(echo "$version" | cut -d. -f2)
  patch=$(echo "$version" | cut -d. -f3)
  next_tag="v${major}.${minor}.$((patch + 1))"
fi

echo "다음 버전: $next_tag (이전: ${latest_tag:-없음})" >&2
echo "$next_tag"
