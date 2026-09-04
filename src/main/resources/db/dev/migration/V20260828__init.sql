create table fcolors
(
    fcolor_id    bigserial   primary key,
    color_name   varchar(50) not null,
    color_code_1 varchar(20) not null,
    color_code_2 varchar(20) not null,
    color_code_3 varchar(20) not null,
    color_code_4 varchar(20) not null
);

create table emotions
(
    emotion_id bigserial    primary key,
    name       varchar(100) not null
);

create table jobs
(
    job_id bigserial    primary key,
    name   varchar(100) not null
);

create table situations
(
    situation_id bigserial    primary key,
    name         varchar(100) not null
);

create table keywords
(
    keyword_id bigserial    primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    name       varchar(100) not null,
    constraint uq_keyword_name unique (name)
);

-- 알림: body는 자유 텍스트, alarm_type은 정해진 값만 허용
create table alarms
(
    alarm_id   bigserial    primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    alarm_type varchar(100) not null
        constraint alarms_alarm_type_check
            check (alarm_type = any (array [
                'LINK_SUMMARY_COMPLETE',
                'FOLDER_DELETED',
                'FOLDER_PERMISSION_CHANGED',
                'CURATION_UPDATED',
                'ANNOUNCEMENT_UPDATE',
                'ANNOUNCEMENT_ERROR'
                ]::varchar[])),
    body       text         not null,
    target_id  bigint       not null,
    title      varchar(100) not null
);

create table curation_section_infos
(
    curation_section_info_id bigserial    primary key,
    created_at               timestamp(6),
    updated_at               timestamp(6),
    base_month               varchar(7)   not null,
    section_number           int          not null,
    title                    varchar(255) not null,
    description              text,
    image_url                text,
    constraint uq_curation_section unique (base_month, section_number)
);

-- 도메인: crawl_strategy는 정해진 값만 허용 (VIDEO 값은 seed 쪽에서 추가됨)
create table domains
(
    domain_id      bigserial    primary key,
    crawl_strategy varchar(50)
        constraint domains_crawl_strategy_check
            check (crawl_strategy = any (array [
                'IFRAME',
                'BODY',
                'DEFAULT'
                ]::varchar[])),
    domain_tail    varchar(255) not null,
    image_url      text,
    name           varchar(100) not null
);

-- ── fcolors 의존 ─────────────────────────────────────────────

create table categories
(
    category_id   bigserial    primary key,
    category_name varchar(100) not null,
    fcolor_id     bigint       not null,
    constraint fk_categories_fcolor
        foreign key (fcolor_id) references fcolors (fcolor_id)
);

-- ── emotions 의존 ─────────────────────────────────────────────

create table curation_ments
(
    curation_ment_id bigserial primary key,
    emotion_id       bigint    not null,
    header_text      text      not null,
    footer_text      text      not null,
    constraint fk_curation_ments_emotion
        foreign key (emotion_id) references emotions (emotion_id)
);

-- ── categories 의존 (self-reference 포함) ─────────────────────

create table folders
(
    folder_id        bigserial    primary key,
    created_at       timestamp(6),
    updated_at       timestamp(6),
    folder_name      varchar(255) not null,
    category_id      bigint       not null,
    parent_folder_id bigint,
    constraint fk_folders_category
        foreign key (category_id) references categories (category_id),
    constraint fk_folders_parent
        foreign key (parent_folder_id) references folders (folder_id)
);

-- ── users (jobs 의존) ─────────────────────────────────────────

create table users
(
    user_id        bigint       not null primary key,
    created_at     timestamp(6),
    updated_at     timestamp(6),
    deleted_reason text,
    gender         varchar(255)
        constraint users_gender_check
            check (gender = any (array ['MALE'::character varying, 'FEMALE'::character varying])),
    inactive_date  timestamp(6),
    nick_name      varchar(255) not null
        constraint uq_users_nick_name unique,
    password       varchar(255) not null,
    role           varchar(255)
        constraint users_role_check
            check (role = any (array ['USER'::character varying, 'ADMIN'::character varying, 'MANAGER'::character varying])),
    status         varchar(255)
        constraint users_status_check
            check (status = any (array ['ACTIVE'::character varying, 'INACTIVE'::character varying, 'TEMP'::character varying])),
    job_id         bigint,
    constraint fk_users_job
        foreign key (job_id) references jobs (job_id)
);

-- ── linkus (categories, domains, emotions, situations 의존) ───
-- 링크: 감정/상황 태그를 가지며, ai_articles와는 단방향 참조만 존재

create table linkus
(
    linku_id         bigserial primary key,
    created_at       timestamp(6),
    updated_at       timestamp(6),
    linku_url        text   not null,
    title            text   not null,
    img_url          text,
    total_view_count bigint not null,
    category_id      bigint not null,
    domain_id        bigint not null,
    emotion_id       bigint not null,
    situation_id     bigint not null,
    constraint uq_linkus_linku_url unique (linku_url),
    constraint fk_linkus_category
        foreign key (category_id) references categories (category_id),
    constraint fk_linkus_domain
        foreign key (domain_id) references domains (domain_id),
    constraint fk_linkus_emotion
        foreign key (emotion_id) references emotions (emotion_id),
    constraint fk_linkus_situation
        foreign key (situation_id) references situations (situation_id)
);

-- ai_articles (linkus 의존): 링크별 AI 요약

create table ai_articles
(
    ai_article_id bigserial    primary key,
    created_at    timestamp(6),
    updated_at    timestamp(6),
    linku_id      bigint       not null,
    summary       varchar(255) not null,
    constraint uq_ai_articles_linku unique (linku_id),
    constraint fk_ai_articles_linku
        foreign key (linku_id) references linkus (linku_id)
);

-- ── linku_keywords (linkus, keywords 의존) ───────────────────

create table linku_keywords
(
    linku_keyword_id bigserial primary key,
    linku_id         bigint    not null,
    keyword_id       bigint    not null,
    constraint uq_linku_keyword unique (linku_id, keyword_id),
    constraint fk_linku_keywords_linku
        foreign key (linku_id) references linkus (linku_id),
    constraint fk_linku_keywords_keyword
        foreign key (keyword_id) references keywords (keyword_id)
);

-- ── situation 크로스 테이블 ───────────────────────────────────

create table situation_categories
(
    situation_category_id bigserial primary key,
    situation_id          bigint    not null,
    category_id           bigint    not null,
    constraint fk_situation_categories_situation
        foreign key (situation_id) references situations (situation_id),
    constraint fk_situation_categories_category
        foreign key (category_id) references categories (category_id)
);

create table situation_jobs
(
    situation_job_id bigserial primary key,
    situation_id     bigint    not null,
    job_id           bigint    not null,
    constraint fk_situation_jobs_situation
        foreign key (situation_id) references situations (situation_id),
    constraint fk_situation_jobs_job
        foreign key (job_id) references jobs (job_id)
);

-- ── users 의존 테이블들 ───────────────────────────────────────

create table alarm_settings
(
    user_id           bigint  not null primary key,
    alarm_all_enabled boolean not null,
    notice_enabled    boolean not null,
    link_enabled      boolean not null,
    curation_enabled  boolean not null,
    folder_enabled    boolean not null,
    constraint fk_alarm_settings_user
        foreign key (user_id) references users (user_id) on delete cascade
);

-- 소셜/일반 로그인 계정: provider는 정해진 값만 허용
create table auth_accounts
(
    social_account_id bigserial    primary key,
    created_at        timestamp(6),
    updated_at        timestamp(6),
    email             varchar(255) not null,
    external_id       text         not null,
    profile_image     text,
    provider          varchar(50)  not null
        constraint auth_accounts_provider_check
            check (provider = any (array [
                'GENERAL',
                'KAKAO',
                'GOOGLE',
                'NAVER'
                ]::varchar[])),
    social_token      text,
    user_id           bigint       not null,
    constraint uq_auth_accounts_provider_external unique (provider, external_id),
    constraint fk_auth_accounts_user
        foreign key (user_id) references users (user_id) on delete cascade
);

create table curations
(
    curation_id bigserial   primary key,
    created_at  timestamp(6),
    updated_at  timestamp(6),
    user_id     bigint      not null,
    base_month  varchar(7)  not null,
    header_ment text,
    footer_ment text,
    constraint fk_curations_user
        foreign key (user_id) references users (user_id) on delete cascade
);

-- interests / purposes: 고정된 값 목록을 관리하는 카탈로그(마스터) 테이블

create table interests
(
    id   bigserial   primary key,
    name varchar(50) not null,
    constraint uk_interests_name unique (name)
);

create table purposes
(
    id   bigserial   primary key,
    name varchar(50) not null,
    constraint uk_purposes_name unique (name)
);

insert into interests (name)
values ('BUSINESS'),
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

insert into purposes (name)
values ('CAREER'),
       ('STUDY'),
       ('WORK'),
       ('SIDE_PROJECT'),
       ('SELF_DEVELOPMENT'),
       ('LATER_READING'),
       ('INSIGHTS'),
       ('CREATION_REFERENCE'),
       ('OTHERS');

-- 약관 동의: terms_type은 정해진 값만 허용
create table terms_agreements
(
    terms_agreement_id bigserial    primary key,
    created_at         timestamp(6),
    updated_at         timestamp(6),
    user_id            bigint       not null,
    terms_type         varchar(50)  not null
        constraint terms_agreements_terms_type_check
            check (terms_type = any (array [
                'TERMS_OF_USE',
                'PRIVACY_POLICY',
                'MARKETING'
                ]::varchar[])),
    is_required        boolean      not null,
    terms_version      varchar(10)  not null,
    agreed_at          timestamp(6) not null,
    is_agreed          boolean      not null,
    constraint uk_terms_agreements_user_type unique (user_id, terms_type),
    constraint fk_terms_agreements_user
        foreign key (user_id) references users (user_id) on delete cascade
);

-- 유저별 알림 수신함: 미읽음 조회용 인덱스 포함
create table user_alarms
(
    user_alarm_id bigserial    primary key,
    created_at    timestamp(6),
    updated_at    timestamp(6),
    user_id       bigint       not null,
    alarm_id      bigint       not null,
    is_read       boolean      not null,
    read_at       timestamp(6),
    delivered_at  timestamp(6) not null,
    constraint uq_user_alarm_user_alarm unique (user_id, alarm_id),
    constraint fk_user_alarms_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_user_alarms_alarm
        foreign key (alarm_id) references alarms (alarm_id)
);

create index idx_user_alarms_unread
    on user_alarms (user_id, is_read, created_at);

create table user_fcm_tokens
(
    user_fcm_token_id bigserial primary key,
    created_at        timestamp(6),
    updated_at        timestamp(6),
    user_id           bigint    not null,
    fcm_token         text      not null,
    last_used_at      timestamp(6),
    expires_at        timestamp(6),
    is_active         boolean,
    constraint fk_user_fcm_tokens_user
        foreign key (user_id) references users (user_id) on delete cascade
);

create table users_category_colors
(
    users_category_color_id bigserial primary key,
    user_id                 bigint    not null,
    category_id             bigint    not null,
    fcolor_id               bigint    not null,
    constraint fk_users_category_colors_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_category_colors_category
        foreign key (category_id) references categories (category_id),
    constraint fk_users_category_colors_fcolor
        foreign key (fcolor_id) references fcolors (fcolor_id)
);

-- 폴더 공유 링크: permission_type은 정해진 값만 허용
create table folder_share_links
(
    folder_share_link_id bigserial    primary key,
    created_at           timestamp(6),
    updated_at           timestamp(6),
    token                varchar(64)  not null unique,
    expires_at           timestamp(6) not null,
    is_active            boolean      not null,
    permission_type      varchar(255) not null
        constraint folder_share_links_permission_type_check
            check (permission_type = any (array [
                'VIEWER',
                'WRITER',
                'OWNER',
                'NONE'
                ]::varchar[])),
    folder_id            bigint       not null,
    creator_id           bigint       not null,
    constraint fk_folder_share_links_folder
        foreign key (folder_id) references folders (folder_id),
    constraint fk_folder_share_links_creator
        foreign key (creator_id) references users (user_id) on delete cascade
);

-- 키워드 월별 집계: type은 정해진 값만 허용
create table keyword_monthly_counts
(
    keyword_monthly_count_id bigserial   primary key,
    user_id                  bigint      not null,
    type                     varchar(10) not null
        constraint keyword_monthly_counts_type_check
            check (type = any (array [
                'EMOTION',
                'SITUATION'
                ]::varchar[])),
    ref_id                   bigint      not null,
    base_month               varchar(7)  not null,
    count                    int         not null,
    constraint uq_keyword_monthly unique (user_id, type, ref_id, base_month),
    constraint fk_keyword_monthly_counts_user
        foreign key (user_id) references users (user_id) on delete cascade
);

-- 유저별 저장 링크: 유저 삭제 시 함께 삭제(CASCADE), user_id 조회용 인덱스 포함
create table users_linkus
(
    user_linku_id   bigserial    primary key,
    created_at      timestamp(6),
    updated_at      timestamp(6),
    user_id         bigint       not null,
    linku_id        bigint       not null,
    emotion_id      bigint       not null,
    situation_id    bigint,
    title           varchar(255),
    memo            varchar(255),
    image_url       text,
    is_ai_exist     boolean      not null,
    is_emotion_ai   boolean      not null,
    is_situation_ai boolean      not null,
    view_count      int          not null,
    last_viewed_at  timestamp(6),
    constraint fk_users_linkus_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_linkus_linku
        foreign key (linku_id) references linkus (linku_id),
    constraint fk_users_linkus_emotion
        foreign key (emotion_id) references emotions (emotion_id),
    constraint fk_users_linkus_situation
        foreign key (situation_id) references situations (situation_id)
);

create index idx_users_linkus_user_id
    on users_linkus (user_id);

-- ── users_linkus 의존 테이블들 ───────────────────────────────

create table linku_folders
(
    linku_folder_id bigserial primary key,
    folder_id       bigint    not null,
    user_linku_id   bigint    not null,
    constraint fk_linku_folders_folder
        foreign key (folder_id) references folders (folder_id),
    constraint fk_linku_folders_users_linku
        foreign key (user_linku_id) references users_linkus (user_linku_id) on delete cascade
);

-- 유저-폴더 권한: permission_type은 정해진 값만 허용
create table users_folders
(
    users_folder_id bigserial    primary key,
    created_at      timestamp(6),
    updated_at      timestamp(6),
    permission_type varchar(255) not null
        constraint users_folders_permission_type_check
            check (permission_type = any (array [
                'VIEWER',
                'WRITER',
                'OWNER',
                'NONE'
                ]::varchar[])),
    is_bookmarked   boolean,
    user_id         bigint       not null,
    folder_id       bigint       not null,
    constraint fk_users_folders_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_folders_folder
        foreign key (folder_id) references folders (folder_id)
);

-- 큐레이션에 포함된 링크: type은 정해진 값만 허용
create table curation_linkus
(
    curation_linku_id bigserial     primary key,
    curation_id       bigint        not null,
    user_linku_id     bigint,
    type              varchar(255)  not null
        constraint curation_linkus_type_check
            check (type = any (array [
                'INTERNAL',
                'EXTERNAL'
                ]::varchar[])),
    url               text,
    title             varchar(255),
    image_url         varchar(1024),
    constraint fk_curation_linkus_curation
        foreign key (curation_id) references curations (curation_id) on delete cascade,
    constraint fk_curation_linkus_users_linku
        foreign key (user_linku_id) references users_linkus (user_linku_id) on delete cascade
);

-- 유저별 검색 기록: 유저 삭제 시 함께 삭제(CASCADE)
create table linku_search_histories
(
    linku_search_history_id bigserial primary key,
    user_id                 bigint       not null,
    keyword                 varchar(200) not null,
    created_at              timestamp,
    updated_at              timestamp,
    constraint fk_linku_search_histories_user
        foreign key (user_id) references users (user_id) on delete cascade
);

create index idx_search_histories_user_id_created_at
    on linku_search_histories (user_id, created_at desc);

-- 유저-관심사, 유저-목적: 다대다 연결 테이블
create table users_interests
(
    id          bigserial    primary key,
    user_id     bigint       not null,
    interest_id bigint       not null,
    selected_at timestamp(6) not null,
    constraint fk_users_interests_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_interests_interest
        foreign key (interest_id) references interests (id),
    constraint uk_users_interests_user_interest unique (user_id, interest_id)
);

create table users_purposes
(
    id          bigserial    primary key,
    user_id     bigint       not null,
    purpose_id  bigint       not null,
    selected_at timestamp(6) not null,
    constraint fk_users_purposes_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_purposes_purpose
        foreign key (purpose_id) references purposes (id),
    constraint uk_users_purposes_user_purpose unique (user_id, purpose_id)
);

-- ── 홈화면 링크 추천용 프로필 테이블 ────────────────────────
create extension if not exists pg_trgm;

create table user_content_profiles
(
    user_id              bigint primary key
        constraint fk_user_content_profiles_user
            references users (user_id) on delete cascade,
    profile_tsquery_text text,
    profile_text         text,
    updated_at           timestamp(6) not null default now()
);

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

create table user_profile_refresh_queue
(
    user_id      bigint primary key
        constraint fk_user_profile_refresh_queue_user
            references users (user_id) on delete cascade,
    requested_at timestamp(6) not null default now()
);

-- ── Spring Batch 메타데이터 스키마 ────────────

CREATE TABLE BATCH_JOB_INSTANCE  (
	JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT ,
	JOB_NAME VARCHAR(100) NOT NULL,
	JOB_KEY VARCHAR(32) NOT NULL,
	constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ;

CREATE TABLE BATCH_JOB_EXECUTION  (
	JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT  ,
	JOB_INSTANCE_ID BIGINT NOT NULL,
	CREATE_TIME TIMESTAMP NOT NULL,
	START_TIME TIMESTAMP DEFAULT NULL ,
	END_TIME TIMESTAMP DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED TIMESTAMP,
	constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
	references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
	JOB_EXECUTION_ID BIGINT NOT NULL ,
	PARAMETER_NAME VARCHAR(100) NOT NULL ,
	PARAMETER_TYPE VARCHAR(100) NOT NULL ,
	PARAMETER_VALUE VARCHAR(2500) ,
	IDENTIFYING CHAR(1) NOT NULL ,
	constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION  (
	STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT NOT NULL,
	STEP_NAME VARCHAR(100) NOT NULL,
	JOB_EXECUTION_ID BIGINT NOT NULL,
	CREATE_TIME TIMESTAMP NOT NULL,
	START_TIME TIMESTAMP DEFAULT NULL ,
	END_TIME TIMESTAMP DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	COMMIT_COUNT BIGINT ,
	READ_COUNT BIGINT ,
	FILTER_COUNT BIGINT ,
	WRITE_COUNT BIGINT ,
	READ_SKIP_COUNT BIGINT ,
	WRITE_SKIP_COUNT BIGINT ,
	PROCESS_SKIP_COUNT BIGINT ,
	ROLLBACK_COUNT BIGINT ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED TIMESTAMP,
	constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
	STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
	references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
	JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
