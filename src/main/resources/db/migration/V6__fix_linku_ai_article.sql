-- 양방향 매핑 제거에 따른 외래키/컬럼 정리 및 ON DELETE CASCADE 추가

------------------------------------------------------------
-- 1. linkus 테이블에서 ai_articles 참조 관계 제거
------------------------------------------------------------
-- 기존 외래키 제약 조건 삭제
ALTER TABLE linkus DROP CONSTRAINT IF EXISTS fk_linkus_ai_article;

-- 불필요해진 ai_article_id 컬럼 삭제 (ai_articles.linku_id 단방향으로 통일)
ALTER TABLE linkus DROP COLUMN IF EXISTS ai_article_id;

------------------------------------------------------------
-- 2. linkus에 emotion, situation추가
------------------------------------------------------------
ALTER TABLE linkus ADD COLUMN IF NOT EXISTS emotion_id bigint;
ALTER TABLE linkus ADD COLUMN IF NOT EXISTS situation_id bigint;

UPDATE linkus SET emotion_id = 2 WHERE emotion_id IS NULL;
UPDATE linkus SET situation_id = 1 WHERE situation_id IS NULL;

-- 외래키 제약조건 설정 (V1에 생성된 emotions, situations 테이블 참조)
ALTER TABLE linkus
    ADD CONSTRAINT fk_linkus_emotion
        FOREIGN KEY (emotion_id) REFERENCES emotions (emotion_id);

ALTER TABLE linkus
    ADD CONSTRAINT fk_linkus_situation
        FOREIGN KEY (situation_id) REFERENCES situations (situation_id);

-- NOT NULL 제약 추가
ALTER TABLE linkus ALTER COLUMN emotion_id SET NOT NULL;
ALTER TABLE linkus ALTER COLUMN situation_id SET NOT NULL;
