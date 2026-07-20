-- interests / purposes 를 유저별 문자열 저장 방식에서 다대다(N:M) 관계로 전환한다.
-- interests / purposes 는 값 카탈로그(마스터) 테이블로 재정의하고,
-- users_interests / users_purposes 조인 테이블로 Users 와 다대다 연결한다.

-- 1. 기존 유저별 선택 테이블을 임시로 옮겨둔다.
ALTER TABLE interests RENAME TO interests_old;
ALTER TABLE purposes RENAME TO purposes_old;

-- 2. 카탈로그(마스터) 테이블 재생성
CREATE TABLE interests
(
    id   bigserial PRIMARY KEY,
    name varchar(50) NOT NULL,
    CONSTRAINT uk_interests_name UNIQUE (name)
);

CREATE TABLE purposes
(
    id   bigserial PRIMARY KEY,
    name varchar(50) NOT NULL,
    CONSTRAINT uk_purposes_name UNIQUE (name)
);

-- 3. 고정 카탈로그 값 시딩 (기존 도메인 enum 값 기준)
INSERT INTO interests (name)
VALUES ('BUSINESS'),
       ('IT'),
       ('DESIGN'),
       ('PSYCHOLOGY'),
       ('CAREER'),
       ('CURRENT_EVENTS'),
       ('STUDY'),
       ('STARTUP'),
       ('SOCIETY'),
       ('WRITING'),
       ('INSIGHTS'),
       ('COLLECT');

INSERT INTO purposes (name)
VALUES ('CAREER'),
       ('STUDY'),
       ('WORK'),
       ('SIDE_PROJECT'),
       ('SELF_DEVELOPMENT'),
       ('LATER_READING'),
       ('INSIGHTS'),
       ('CREATION_REFERENCE'),
       ('OTHERS');

-- 4. 기존 데이터에 남아있던, 고정 카탈로그에 없는 값(자유 입력 값)도 유실 없이 카탈로그에 편입
INSERT INTO interests (name)
SELECT DISTINCT interest
FROM interests_old
WHERE interest IS NOT NULL
ON CONFLICT (name) DO NOTHING;

INSERT INTO purposes (name)
SELECT DISTINCT purpose
FROM purposes_old
WHERE purpose IS NOT NULL
ON CONFLICT (name) DO NOTHING;

-- 5. 조인(다대다) 테이블 생성
CREATE TABLE users_interests
(
    id          bigserial    PRIMARY KEY,
    user_id     bigint       NOT NULL,
    interest_id bigint       NOT NULL,
    selected_at timestamp(6) NOT NULL,
    CONSTRAINT fk_users_interests_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_users_interests_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id),
    CONSTRAINT uk_users_interests_user_interest UNIQUE (user_id, interest_id)
);

CREATE TABLE users_purposes
(
    id          bigserial    PRIMARY KEY,
    user_id     bigint       NOT NULL,
    purpose_id  bigint       NOT NULL,
    selected_at timestamp(6) NOT NULL,
    CONSTRAINT fk_users_purposes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_users_purposes_purpose
        FOREIGN KEY (purpose_id) REFERENCES purposes (id),
    CONSTRAINT uk_users_purposes_user_purpose UNIQUE (user_id, purpose_id)
);

-- 6. 기존 유저별 선택 데이터를 조인 테이블로 이관
INSERT INTO users_interests (user_id, interest_id, selected_at)
SELECT io.user_id, i.id, io.selected_at
FROM interests_old io
         JOIN interests i ON i.name = io.interest;

INSERT INTO users_purposes (user_id, purpose_id, selected_at)
SELECT po.user_id, p.id, po.selected_at
FROM purposes_old po
         JOIN purposes p ON p.name = po.purpose;

-- 7. 임시 테이블 정리
DROP TABLE interests_old;
DROP TABLE purposes_old;
