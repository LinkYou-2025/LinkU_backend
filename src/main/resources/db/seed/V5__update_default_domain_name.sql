-- 알 수 없는 도메인의 표시 이름을 '웹사이트'로 변경
UPDATE domains SET name = '웹사이트' WHERE domain_id = 1;

-- 2) 기존 도메인 보정
UPDATE domains SET name = 'blog', crawlstrategy = 'IFRAME' WHERE domaintail = 'blog.naver.com';
UPDATE domains SET name = 'blog', crawlstrategy = 'DEFAULT' WHERE domaintail = 'cafe.naver.com';
UPDATE domains SET name = 'blog', crawlstrategy = 'DEFAULT' WHERE domaintail = 'news.naver.com';
UPDATE domains SET name = 'naver shopping', crawlstrategy = 'DEFAULT' WHERE domaintail = 'smartstore.naver.com';
UPDATE domains SET name = 'naver shopping', crawlstrategy = 'DEFAULT' WHERE domain_tail = 'm.shopping.naver.com';
UPDATE domains SET name = 'naver shopping', crawlstrategy = 'DEFAULT' WHERE domain_tail = 'smartstore.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'map.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'm.map.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'comic.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'm.comic.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'naver.me';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'dict.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'ko.dict.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'en.dict.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'newsstand.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'm.newsstandnaver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'novel.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'series.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'DEFAULT' WHERE domaintail = 'tv.naver.com';
UPDATE domains SET name = 'naver', crawlstrategy = 'IFRAME' WHERE domaintail = 'kin.naver.com';

-- 영상 도메인
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'youtube.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'tiktok.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'tv.naver.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'twitch.tv';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'afreecatv.com';
UPDATE domains SET crawl_strategy = 'VIDEO' WHERE domain_tail = 'loom.com';

-- 서비스종료된 도메인
DELETE FROM domains WHERE domaintail = 'post.naver.com';
DELETE FROM domains WHERE domaintail = 'm.post.naver.com';
DELETE FROM domains WHERE domaintail = 'blogon.naver.com';
