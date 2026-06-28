-- =========================================================
-- V1__init.sql  (PostgreSQL)
-- MySQL → PostgreSQL 전환 + 엔티티 @Table(name) 기준으로 전체 재작성
-- =========================================================

-- ── 의존성 없는 테이블 ───────────────────────────────────────

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

create table alarms
(
    alarm_id   bigserial    primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    alarm_type varchar(100) not null,
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

create table domains
(
    domain_id      bigserial    primary key,
    crawl_strategy varchar(50),
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
        constraint ukpl4047a5k5enw6up4sjs8lyut unique,
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

-- ── linkus (categories, domains 의존) ────────────────────────
-- ai_article_id 컬럼 제거 — ai_articles.linku_id 단방향으로 truth source 통일

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
    constraint fk_linkus_category
        foreign key (category_id) references categories (category_id),
    constraint fk_linkus_domain
        foreign key (domain_id) references domains (domain_id)
);

-- ── ai_articles (linkus 의존) ────────────────────────────────

create table ai_articles
(
    ai_article_id bigserial    primary key,
    created_at    timestamp(6),
    updated_at    timestamp(6),
    linku_id      bigint       not null,
    title         text         not null,
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

-- AlarmSetting은 @MapsId(@OneToOne) → PK = user_id (bigserial 없음)
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

create table auth_accounts
(
    social_account_id bigserial    primary key,
    created_at        timestamp(6),
    updated_at        timestamp(6),
    email             varchar(255) not null,
    external_id       text         not null,
    profile_image     text,
    provider          varchar(50)  not null,
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

create table interests
(
    id          bigserial    primary key,
    interest    varchar(255) not null,
    user_id     bigint       not null,
    selected_at timestamp(6) not null,
    constraint fk_interests_user
        foreign key (user_id) references users (user_id) on delete cascade
);

create table purposes
(
    id          bigserial    primary key,
    purpose     varchar(255) not null,
    user_id     bigint       not null,
    selected_at timestamp(6) not null,
    constraint fk_purposes_user
        foreign key (user_id) references users (user_id) on delete cascade
);

create table terms_agreements
(
    terms_agreement_id bigserial    primary key,
    created_at         timestamp(6),
    updated_at         timestamp(6),
    user_id            bigint       not null,
    terms_type         varchar(50)  not null,
    is_required        boolean      not null,
    terms_version      varchar(10)  not null,
    agreed_at          timestamp(6) not null,
    is_agreed          boolean      not null,
    constraint uk_terms_agreements_user_type unique (user_id, terms_type),
    constraint fk_terms_agreements_user
        foreign key (user_id) references users (user_id) on delete cascade
);

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

create table folder_share_links
(
    folder_share_link_id bigserial    primary key,
    created_at           timestamp(6),
    updated_at           timestamp(6),
    token                varchar(64)  not null unique,
    expires_at           timestamp(6) not null,
    is_active            boolean      not null,
    permission_type      varchar(255) not null,
    folder_id            bigint       not null,
    creator_id           bigint       not null,
    constraint fk_folder_share_links_folder
        foreign key (folder_id) references folders (folder_id),
    constraint fk_folder_share_links_creator
        foreign key (creator_id) references users (user_id) on delete cascade
);

create table keyword_monthly_counts
(
    keyword_monthly_count_id bigserial   primary key,
    user_id                  bigint      not null,
    type                     varchar(10) not null,
    ref_id                   bigint      not null,
    base_month               varchar(7)  not null,
    count                    int         not null,
    constraint uq_keyword_monthly unique (user_id, type, ref_id, base_month),
    constraint fk_keyword_monthly_counts_user
        foreign key (user_id) references users (user_id) on delete cascade
);

-- ── users_linkus (users, linkus, emotions, situations 의존) ──

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

create table users_folders
(
    users_folder_id bigserial    primary key,
    created_at      timestamp(6),
    updated_at      timestamp(6),
    permission_type varchar(255) not null,
    is_bookmarked   boolean,
    user_id         bigint       not null,
    folder_id       bigint       not null,
    constraint fk_users_folders_user
        foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_users_folders_folder
        foreign key (folder_id) references folders (folder_id)
);

create table curation_linkus
(
    curation_linku_id bigserial     primary key,
    curation_id       bigint        not null,
    user_linku_id     bigint,
    type              varchar(255)  not null,
    url               text,
    title             varchar(255),
    image_url         varchar(1024),
    url_normalized    varchar(2048),
    constraint fk_curation_linkus_curation
        foreign key (curation_id) references curations (curation_id) on delete cascade,
    constraint fk_curation_linkus_users_linku
        foreign key (user_linku_id) references users_linkus (user_linku_id) on delete cascade
);
