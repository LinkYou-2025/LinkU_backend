-- 홈화면 추천 테스트용 시드 유저(V15__seed_users.sql)에 로그인 가능한 이메일을 연결한다.
-- V15/V16이 users/users_linkus까지는 채워뒀지만 auth_accounts(GENERAL)가 없어서
-- POST /api/v1/auth/login으로 실제 로그인할 방법이 없었다(email 기준으로 auth_accounts를 조회하기 때문).
--
-- password는 V15에 이미 저장된 것과 동일한 bcrypt 해시를 그대로 쓰므로 아래 계정으로 바로
-- 로그인 가능하다 (평문 비밀번호는 보안상 커밋에 남기지 않음 — 팀 내부 문서 참고).
--
-- seed_it_dev(103)만 우선 연결해둔다 — IT·개발 카테고리 링크가 12개 저장돼 있어
-- GET /api/v1/linku/recommend 테스트 조건(job 설정됨, 저장 링크 3개 이상)을 이미 만족한다.
-- 다른 카테고리로 테스트하고 싶으면 동일 패턴으로 user_id만 바꿔 추가하면 된다
-- (101 seed_lang_learner, 102 seed_studymethod, 104~113 등 V15 참고).

INSERT INTO auth_accounts (email, external_id, provider, user_id, created_at, updated_at)
SELECT 'seed103@test.com', 'seed103@test.com', 'GENERAL', 103, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_accounts WHERE provider = 'GENERAL' AND external_id = 'seed103@test.com'
)
AND EXISTS (SELECT 1 FROM users WHERE user_id = 103);
