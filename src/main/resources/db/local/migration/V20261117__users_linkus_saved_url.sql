-- 사용자가 실제로 저장한 URL. linkus.linku_url(대표 URL)과 달라도 열기 동작에는 이 값을 쓴다.
alter table users_linkus add column saved_url text;

update users_linkus ul
set saved_url = l.linku_url
from linkus l
where l.linku_id = ul.linku_id;

alter table users_linkus alter column saved_url set not null;

-- saved_url을 대표 URL alias로 조회한다 (단축/모바일 URL 재저장 시 외부 요청 생략).
-- 등치 조회만 하고, 긴 URL이 btree 행 크기 제한에 걸리지 않도록 hash 인덱스를 쓴다.
create index idx_users_linkus_saved_url on users_linkus using hash (saved_url);
