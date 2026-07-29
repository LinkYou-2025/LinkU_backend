-- 홈화면 추천 로직 검증용 테스트 계정.
-- 실서비스 콘텐츠가 아닌 로컬 전용 데이터라 db/local(= application-local.yml에서만 flyway locations에 포함)에 둔다.
-- db/seed(공통 마스터 데이터, 모든 환경에 적용)와는 목적이 다르므로 절대 섞지 않는다.
--
-- user_id는 Users.id가 @Tsid로 생성되는 값(실제로는 매우 큰 64bit 정수)과 겹치지 않도록,
-- 눈에 띄게 작은 고정값(1)을 예약해서 쓴다.
--
-- role은 일반 USER로 둔다 — 이 계정의 용도는 홈 추천(일반 유저 기능) 테스트뿐이라
-- MANAGER/ADMIN 권한이 필요 없다(둘 다 /api/v1/manage/**, /admin/** 같은 관리자 전용
-- 엔드포인트 접근용이라 이 목적과 무관).
--
-- job_id=3(직장인)로 잡아둔다 — LinkuRecommendService가 job이 null이면
-- JOB_NOT_SET 예외를 던지므로 추천을 테스트하려면 필수.
--
-- password는 로그인 테스트가 필요할 경우를 대비해 bcrypt로 실제 인코딩해둔다 (평문: test1234!).

INSERT INTO users (user_id, nick_name, password, role, status, job_id, created_at, updated_at)
VALUES (
    1,
    'home_reco_test_user',
    '$2b$12$auCAmviSO58zXW2roqtSj.ASzpINp7F5GyaOtL9ZauTIYEQwEhHFK',
    'USER',
    'ACTIVE',
    3,
    now(),
    now()
)
ON CONFLICT (user_id) DO NOTHING;
