#!/usr/bin/env bash
# 원격 DB의 특정 계정(user_id) 데이터 + 그 계정이 저장한 링크와 연관 테이블만 골라
# 로컬 Postgres로 옮기는 스크립트.
#
# 사용법:
#   1) 아래 REMOTE_DSN / LOCAL_DSN / TARGET_USER_ID 세 값을 채운다.
#      (DSN 예: "postgresql://USER:PASSWORD@HOST:5432/DBNAME")
#   2) 로컬 DB에 스키마가 이미 있어야 한다 (로컬 스프링부트를 한 번 띄워서 Flyway 마이그레이션 +
#      V2/V14~V17 시드가 돌게 하면 됨 — 뜨우고 바로 꺼도 됨).
#   3) TARGET_USER_ID=123 ./export_recommend_account.sh 실행
#
# 동작 방식:
#   - 참조/마스터 테이블(fcolors/emotions/situations/jobs/domains/categories/keywords/
#     situation_categories/situation_jobs)은 로컬도 V2 시드로 이미 채워져 있다고 가정하고 건드리지
#     않는다. 원격에만 있고 로컬엔 없는 category/domain이 있으면 아래 STEP0을 켜서 같이 옮기면 된다.
#   - 나머지 테이블은 임시 스테이징 테이블에 COPY한 다음 INSERT ... ON CONFLICT DO NOTHING으로
#     본 테이블에 합친다 — 재실행해도 안전하고, PK가 이미 로컬에 있어도 에러 없이 건너뛴다.
#   - FK 순서를 일일이 맞추지 않아도 되도록 로컬 세션에서 session_replication_role=replica로
#     제약조건 트리거를 잠깐 꺼둔다(끝나면 원복).

set -euo pipefail

REMOTE_DSN="${REMOTE_DSN:-postgresql://USER:PASSWORD@REMOTE_HOST:5432/DBNAME}"
LOCAL_DSN="${LOCAL_DSN:-postgresql://USER:PASSWORD@localhost:5432/linkUDB}"
TARGET_USER_ID="${TARGET_USER_ID:-}"
# 원격 카테고리/도메인이 로컬 시드와 다를 수 있어서, 필요하면 1로 켜서 마스터 테이블도 같이 합친다.
COPY_MASTER_TABLES="${COPY_MASTER_TABLES:-0}"

if [[ -z "$TARGET_USER_ID" ]]; then
  echo "TARGET_USER_ID를 지정하세요. 예: TARGET_USER_ID=123 ./export_recommend_account.sh" >&2
  exit 1
fi

# select_sql 결과를 로컬 target_table에 ON CONFLICT DO NOTHING으로 합친다.
# pk_cols: 콤마 구분 PK 컬럼(복합키면 "col1,col2")
upsert_table() {
  local select_sql="$1"
  local target_table="$2"
  local pk_cols="$3"
  local staging="stg_${target_table}"

  echo ">> ${target_table}"
  psql "$LOCAL_DSN" -q -c "CREATE TEMP TABLE ${staging} (LIKE ${target_table} INCLUDING ALL);"
  psql "$REMOTE_DSN" -c "\\copy (${select_sql}) TO STDOUT" \
    | psql "$LOCAL_DSN" -q -c "\\copy ${staging} FROM STDIN"
  psql "$LOCAL_DSN" -q -c "
    INSERT INTO ${target_table}
    SELECT * FROM ${staging}
    ON CONFLICT (${pk_cols}) DO NOTHING;
    DROP TABLE ${staging};
  "
}

echo "== 로컬 DB 제약조건 트리거 임시 해제 =="
trap 'psql "$LOCAL_DSN" -c "SET session_replication_role = DEFAULT;" >/dev/null' EXIT
psql "$LOCAL_DSN" -c "SET session_replication_role = replica;" -c "SELECT 1;" >/dev/null

if [[ "$COPY_MASTER_TABLES" == "1" ]]; then
  echo "== 0. 참조/마스터 테이블 (원격 기준 병합) =="
  upsert_table "SELECT * FROM fcolors"              fcolors              fcolor_id
  upsert_table "SELECT * FROM emotions"             emotions             emotion_id
  upsert_table "SELECT * FROM jobs"                 jobs                 job_id
  upsert_table "SELECT * FROM situations"           situations           situation_id
  upsert_table "SELECT * FROM keywords"             keywords             keyword_id
  upsert_table "SELECT * FROM domains"              domains              domain_id
  upsert_table "SELECT * FROM categories"           categories           category_id
  upsert_table "SELECT * FROM situation_categories" situation_categories situation_category_id
  upsert_table "SELECT * FROM situation_jobs"       situation_jobs       situation_job_id
fi

echo "== 1. 대상 유저 =="
upsert_table "SELECT * FROM users WHERE user_id = ${TARGET_USER_ID}" users user_id

echo "== 2. 대상 유저가 저장한 링크(users_linkus) + 링크 원본(linkus) + 부가정보 =="
upsert_table "
  SELECT * FROM linkus
  WHERE linku_id IN (SELECT linku_id FROM users_linkus WHERE user_id = ${TARGET_USER_ID})
" linkus linku_id

upsert_table "
  SELECT * FROM ai_articles
  WHERE linku_id IN (SELECT linku_id FROM users_linkus WHERE user_id = ${TARGET_USER_ID})
" ai_articles ai_article_id

upsert_table "SELECT * FROM users_linkus WHERE user_id = ${TARGET_USER_ID}" users_linkus user_linku_id

upsert_table "
  SELECT * FROM linku_keywords
  WHERE linku_id IN (SELECT linku_id FROM users_linkus WHERE user_id = ${TARGET_USER_ID})
" linku_keywords linku_keyword_id

echo "== 3. 폴더 (users_folders로 소유한 폴더 + linku_folders로 걸린 폴더) =="
upsert_table "
  SELECT * FROM folders
  WHERE folder_id IN (
    SELECT folder_id FROM users_folders WHERE user_id = ${TARGET_USER_ID}
    UNION
    SELECT folder_id FROM linku_folders
    WHERE user_linku_id IN (SELECT user_linku_id FROM users_linkus WHERE user_id = ${TARGET_USER_ID})
  )
" folders folder_id

upsert_table "SELECT * FROM users_folders WHERE user_id = ${TARGET_USER_ID}" users_folders users_folder_id

upsert_table "
  SELECT * FROM linku_folders
  WHERE user_linku_id IN (SELECT user_linku_id FROM users_linkus WHERE user_id = ${TARGET_USER_ID})
" linku_folders linku_folder_id

echo "== 4. 추천 스코어링용 프로필 (있으면) =="
upsert_table "SELECT * FROM user_content_profiles WHERE user_id = ${TARGET_USER_ID}" user_content_profiles user_id
upsert_table "SELECT * FROM user_profile_keywords WHERE user_id = ${TARGET_USER_ID}" user_profile_keywords user_profile_keyword_id

echo "== 5. bigserial 시퀀스를 복사된 최댓값 이후로 맞추기 (안 하면 다음 INSERT에서 PK 충돌 가능) =="
for t in "linkus:linku_id" "ai_articles:ai_article_id" "users_linkus:user_linku_id" \
         "linku_keywords:linku_keyword_id" "folders:folder_id" "users_folders:users_folder_id" \
         "linku_folders:linku_folder_id" "user_profile_keywords:user_profile_keyword_id"; do
  table="${t%%:*}"; col="${t##*:}"
  psql "$LOCAL_DSN" -q -c "
    SELECT setval(pg_get_serial_sequence('${table}', '${col}'),
                   GREATEST((SELECT COALESCE(MAX(${col}), 1) FROM ${table}), 1));
  "
done

echo "== 완료 =="
