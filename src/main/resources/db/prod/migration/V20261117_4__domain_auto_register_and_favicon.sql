-- 도메인 행 출처: MANUAL(사람이 관리) / AUTO(처음 보는 사이트 자동 등록). 기존 행은 모두 MANUAL.
alter table domains add column source varchar(20) not null default 'MANUAL';

-- 파비콘 수집 실패 시 다음 시도 시각. null이면 바로 수집 대상이다.
alter table domains add column favicon_next_attempt_at timestamp(6);

-- 동시 저장 요청이 같은 도메인을 중복 등록하지 않도록 DB에서 막는다.
-- 배포 전 점검: select domain_tail, count(*) from domains group by 1 having count(*) > 1;
create unique index uq_domains_domain_tail on domains (domain_tail);
