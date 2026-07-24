-- 홈화면 링크 추천(recommendLinku) 최적화용 인덱스
-- 쿼리: WHERE user_id = ? (COUNT / 추천 후보 조회 모두 user_id로 필터링)
-- FK는 자동으로 인덱스를 생성하지 않으므로 명시적으로 추가한다.
CREATE INDEX IF NOT EXISTS idx_users_linkus_user_id
    ON users_linkus (user_id);
