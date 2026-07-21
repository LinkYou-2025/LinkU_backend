-- 양방향 매핑 제거에 따른 외래키/컬럼 정리 및 ON DELETE CASCADE 추가

------------------------------------------------------------
-- 1. linkus 테이블에서 ai_articles 참조 관계 제거
------------------------------------------------------------
ALTER TABLE linkus DROP CONSTRAINT IF EXISTS fk_linkus_ai_article;
ALTER TABLE linkus DROP COLUMN IF EXISTS ai_article_id;

------------------------------------------------------------
-- 2. linkus에 emotion, situation 추가
------------------------------------------------------------
ALTER TABLE linkus ADD COLUMN IF NOT EXISTS emotion_id bigint;
ALTER TABLE linkus ADD COLUMN IF NOT EXISTS situation_id bigint;

UPDATE linkus SET emotion_id = 2 WHERE emotion_id IS NULL;
UPDATE linkus SET situation_id = 1 WHERE situation_id IS NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_linkus_emotion') THEN
        ALTER TABLE linkus
            ADD CONSTRAINT fk_linkus_emotion
                FOREIGN KEY (emotion_id) REFERENCES emotions (emotion_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_linkus_situation') THEN
        ALTER TABLE linkus
            ADD CONSTRAINT fk_linkus_situation
                FOREIGN KEY (situation_id) REFERENCES situations (situation_id);
    END IF;
END $$;

ALTER TABLE linkus ALTER COLUMN emotion_id SET NOT NULL;
ALTER TABLE linkus ALTER COLUMN situation_id SET NOT NULL;

DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'linkus' AND column_name = 'linku_url'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_linkus_linku_url'
    ) THEN
        ALTER TABLE linkus ADD CONSTRAINT uq_linkus_linku_url UNIQUE (linku_url);
    END IF;
END $$;
