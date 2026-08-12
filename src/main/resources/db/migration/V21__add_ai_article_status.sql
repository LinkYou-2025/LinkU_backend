-- ai_articles에 생성 상태(PENDING/DONE/FAILED) 컬럼을 추가한다.
-- POST /aiarticle/{linkuid}가 크롤링+Gemini 호출을 동기로 처리하던 것을 비동기로 바꾸면서,
-- "생성 중" 상태를 DB에 실제로 기록해야 GET /aiarticle/{linkuid}로 폴링(PENDING -> DONE/FAILED)이 가능해진다.

-- summary는 PENDING 상태에서는 아직 비어있으므로 NOT NULL 제약을 푼다.
ALTER TABLE ai_articles ALTER COLUMN summary DROP NOT NULL;

ALTER TABLE ai_articles ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'PENDING';
-- 실패 사유 코드(예: CRAWLER4031, GEMINI5041)를 저장해 프론트가 재시도 가능 여부를 판단할 수 있게 한다.
ALTER TABLE ai_articles ADD COLUMN IF NOT EXISTS fail_reason varchar(50);

-- 기존에 이미 summary가 채워져 있던 행은 전부 완료 상태로 백필한다.
UPDATE ai_articles SET status = 'DONE' WHERE summary IS NOT NULL AND summary <> '';

-- 새로 추가되는 행은 애플리케이션(엔티티)이 항상 명시적으로 status를 지정하므로 DB 기본값은 제거한다.
ALTER TABLE ai_articles ALTER COLUMN status DROP DEFAULT;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ai_articles_status_check') THEN
        ALTER TABLE ai_articles
            ADD CONSTRAINT ai_articles_status_check
                CHECK (status = ANY (ARRAY [
                    'PENDING',
                    'DONE',
                    'FAILED'
                    ]::varchar[]));
    END IF;
END $$;
