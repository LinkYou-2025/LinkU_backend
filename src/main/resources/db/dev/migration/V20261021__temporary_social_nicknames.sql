UPDATE users
SET nick_name = 'LINKU_' || md5(random()::text || clock_timestamp()::text || user_id::text) || '_' || user_id
WHERE status = 'TEMP';
