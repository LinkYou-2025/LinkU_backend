-- 링크 수정 API가 공용 linkus 테이블을 직접 건드리지 않도록,
-- users_linkus에 사용자 개인 url 컬럼을 추가한다.
-- (title과 동일한 패턴: 값이 있으면 우선 사용, 없으면 linkus.linku_url로 폴백)
ALTER TABLE users_linkus ADD COLUMN IF NOT EXISTS url text;

-- 기존 row는 현재 연결된 linkus.linku_url 값으로 백필하여
-- 신규/기존 데이터 모두 users_linkus.url이 항상 채워지도록 한다.
UPDATE users_linkus ul
SET url = l.linku_url
FROM linkus l
WHERE ul.linku_id = l.linku_id
  AND ul.url IS NULL;
