-- ai_articles.title 컬럼 제거
-- 생성 시점의 linku.title 스냅샷을 저장할 뿐, 이후 아무 코드에서도 읽지 않는 죽은 컬럼이었음
-- (응답 DTO 미포함, 갱신 로직 없음, 참조는 주석 처리된 죽은 코드뿐)
ALTER TABLE ai_articles
    DROP COLUMN IF EXISTS title;
