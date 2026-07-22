-- 회원 탈퇴(14일 유예 후 스케줄러의 물리 삭제) 시 개인 데이터가 고아 레코드로 남지 않도록
-- users 를 참조하는 FK 중 ON DELETE CASCADE 가 누락된 것을 보강한다.
--
-- 대상
--   - users_linkus.user_id  : 유저가 저장한 개인 링크 기록. CASCADE 누락 상태였음.
--     -> linku_folders.user_linku_id, curation_linkus.user_linku_id 는 이미
--        users_linkus 를 ON DELETE CASCADE 로 참조하고 있어(V1), 이번 변경으로
--        유저 삭제 -> users_linkus 삭제 -> linku_folders/curation_linkus 삭제까지 연쇄적으로 처리된다.
--   - linku_search_histories.user_id : 유저의 개인 검색 기록. 애초에 FK 자체가 없어 정합성이
--     보장되지 않았으므로 FK + CASCADE 를 새로 추가한다.
--
-- interests / purposes 는 V9 에서 이미 유저 개인 데이터가 아닌 마스터(카탈로그) 테이블로 전환되었고,
-- 실제 유저 연결은 users_interests / users_purposes 조인 테이블이 담당하며 두 테이블 모두
-- 생성 시점부터 ON DELETE CASCADE 로 users 를 참조하고 있어 별도 조치가 필요 없다.
--
-- linkus, ai_articles, domains 등 공용(참조) 데이터와 folders 소유자 탈퇴 정책은 이번 변경 대상이 아니다.

-- 1. users_linkus.user_id 에 ON DELETE CASCADE 추가
--    (기존 FK는 CASCADE 옵션이 없어 재생성한다)
ALTER TABLE users_linkus DROP CONSTRAINT IF EXISTS fk_users_linkus_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_linkus_user') THEN
        ALTER TABLE users_linkus
            ADD CONSTRAINT fk_users_linkus_user
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
    END IF;
END $$;

-- 2. linku_search_histories.user_id 에 FK(+ CASCADE) 신규 추가
--    선행 조건: FK가 없어 유실되었던 고아 데이터(이미 삭제된 유저의 검색 기록)를 먼저 정리한다.
DELETE FROM linku_search_histories lsh
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.user_id = lsh.user_id
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_linku_search_histories_user') THEN
        ALTER TABLE linku_search_histories
            ADD CONSTRAINT fk_linku_search_histories_user
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
    END IF;
END $$;
