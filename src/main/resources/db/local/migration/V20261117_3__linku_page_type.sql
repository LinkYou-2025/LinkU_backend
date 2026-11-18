-- 링크 저장 시 AI가 판별한 페이지 종류 (CONTENT / LOGIN / ERROR / PAYWALL / OTHER).
-- null: 판별 안 됨(기존 데이터 또는 판별 실패). 요약 API는 null과 CONTENT만 요약한다.
alter table linkus add column page_type varchar(20);

-- 마지막 판별 시점. 비콘텐츠 판별이 오래되면 요약 요청 시 다시 확인한다.
alter table linkus add column page_type_checked_at timestamp(6);
