-- 홈화면 링크 추천 TextMatch/KeywordMatch(content_score) 를 위한 사전계산 프로필 테이블.
-- service/common/README.md 참고.

-- trgm fallback(정확히 겹치는 단어가 없을 때 사용할 문자 단위 유사도)용 확장.
-- RDS 파라미터 그룹에서 pg_trgm 확장이 허용돼 있어야 한다(AWS RDS Postgres는 기본 허용 목록에 포함).
create extension if not exists pg_trgm;

-- 유저별 title/summary 프로필. profile_tsquery_text 는 "단어1 | 단어2 | ..." 형태로 저장해두고
-- 요청 시점에 to_tsquery('simple', profile_tsquery_text) 로 캐스팅해서 ts_rank_cd 에 사용한다.
-- profile_text 는 ts_rank_cd 가 0(정확히 겹치는 단어 없음)일 때 pg_trgm similarity() fallback 용 원문이다.
create table user_content_profiles
(
    user_id              bigint primary key
        constraint fk_user_content_profiles_user
            references users (user_id) on delete cascade,
    profile_tsquery_text text,
    profile_text         text,
    updated_at           timestamp(6) not null default now()
);

-- 유저별 상위 키워드(태그) 빈도. 후보 링크의 linku_keywords 와 스칼라 서브쿼리로 겹쳐서
-- KeywordMatch 점수를 계산하는 데 쓴다. (지금은 intra-user 신호 — 협업 필터링용 inter-user 신호와는 별개)
-- linku_keywords(uq_linku_keyword)와 같은 방식으로 synthetic PK + unique 제약을 쓴다.
create table user_profile_keywords
(
    user_profile_keyword_id bigserial primary key,
    user_id                 bigint not null
        constraint fk_user_profile_keywords_user
            references users (user_id) on delete cascade,
    keyword_id              bigint not null
        constraint fk_user_profile_keywords_keyword
            references keywords (keyword_id) on delete cascade,
    weight                  int    not null,
    constraint uq_user_profile_keyword unique (user_id, keyword_id)
);

-- 프로필 재계산이 필요한 유저를 표시해두는 dirty queue.
-- 링크 저장/키워드 태깅 완료 시점에 upsert 되고, 워커가 chunk 단위로 드레인 후 삭제한다.
-- (전체 유저 스캔으로 "누가 바뀌었는지" 추론하지 않기 위한 테이블 — service/common/README.md 참고)
create table user_profile_refresh_queue
(
    user_id      bigint primary key
        constraint fk_user_profile_refresh_queue_user
            references users (user_id) on delete cascade,
    requested_at timestamp(6) not null default now()
);
