-- 양방향 매핑 제거에 따른 외래키/컬럼 정리 및 ON DELETE CASCADE 추가

------------------------------------------------------------
-- 1. linkus 테이블에서 ai_articles 참조 관계 제거
------------------------------------------------------------
-- 기존 외래키 제약 조건 삭제
ALTER TABLE linkus DROP CONSTRAINT IF EXISTS fk_linkus_ai_article;

-- 불필요해진 ai_article_id 컬럼 삭제 (ai_articles.linku_id 단방향으로 통일)
ALTER TABLE linkus DROP COLUMN IF EXISTS ai_article_id;

------------------------------------------------------------
-- 2. linkus에 situation추가
------------------------------------------------------------
