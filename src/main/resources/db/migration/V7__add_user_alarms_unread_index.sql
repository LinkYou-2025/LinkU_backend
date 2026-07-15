-- 미읽음 알림 존재 여부 조회(hasUnreadAlarm) 최적화용 복합 인덱스
-- 쿼리: WHERE user_id = ? AND is_read = false AND created_at > ?
-- 컬럼 순서: 동등조건(user_id) -> 동등조건(is_read) -> 범위조건(created_at)
CREATE INDEX IF NOT EXISTS idx_user_alarms_unread
    ON user_alarms (user_id, is_read, created_at);
