-- UserProfileRefreshWorker 재시도 제한용 실패 카운터.
-- 지금은 특정 유저에서 계속 예외가 나도 큐에서 안 지워지고 매 5분 드레인마다 무한 재시도된다 —
-- requested_at 오름차순이라 이런 유저가 청크 앞쪽을 계속 차지해 정상 유저 처리를 밀어낼 수 있다.
-- failure_count가 MAX_FAILURE_COUNT(UserProfileRefreshWorker 참고)에 도달하면 큐에서 포기(삭제)한다.
-- 재요청(enqueue)이 들어오면 요청 자체가 새로운 것이므로 0으로 리셋된다.

ALTER TABLE user_profile_refresh_queue
    ADD COLUMN IF NOT EXISTS failure_count integer NOT NULL DEFAULT 0;
