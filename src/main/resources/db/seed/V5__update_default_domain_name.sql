-- 제약조건에 VIDEO추가
ALTER TABLE domains
DROP CONSTRAINT IF EXISTS domains_crawl_strategy_check;

ALTER TABLE domains
    ADD CONSTRAINT domains_crawl_strategy_check
        CHECK (crawl_strategy IN ('DEFAULT', 'IFRAME', 'VIDEO'));

-- 알 수 없는 도메인의 표시 이름을 '웹사이트'로 변경
UPDATE domains SET name = '웹사이트' WHERE domain_id = 1;

-- 2) 기존 도메인 보정
UPDATE domains SET name = 'blog', crawl_strategy = 'IFRAME' WHERE domain_tail = 'blog.naver.com';
UPDATE domains SET name = 'blog', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'cafe.naver.com';
UPDATE domains SET name = 'blog', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'news.naver.com';
UPDATE domains SET name = 'naver shopping', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'smartstore.naver.com';
UPDATE domains SET name = 'naver shopping', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'm.shopping.naver.com';
UPDATE domains SET name = 'naver shopping', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'smartstore.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'map.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'm.map.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'comic.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'm.comic.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'naver.me';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'dict.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'ko.dict.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'en.dict.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'newsstand.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'm.newsstandnaver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'novel.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'series.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'DEFAULT' WHERE domain_tail = 'tv.naver.com';
UPDATE domains SET name = 'naver', crawl_strategy = 'IFRAME' WHERE domain_tail = 'kin.naver.com';

-- 영상 도메인
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'youtube.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'tiktok.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'tv.naver.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'twitch.tv';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'afreecatv.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'loom.com';

-- 서비스종료된 도메인
DELETE FROM domains WHERE domain_tail = 'post.naver.com';
DELETE FROM domains WHERE domain_tail = 'm.post.naver.com';
DELETE FROM domains WHERE domain_tail = 'blogon.naver.com';
