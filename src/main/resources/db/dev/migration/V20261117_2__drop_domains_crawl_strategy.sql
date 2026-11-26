-- 사이트별 본문 추출 방식은 DB 설정이 아니라 코드(사이트 전용 추출기)에서 정한다. 더 이상 쓰지 않는 컬럼.
alter table domains drop column crawl_strategy;
