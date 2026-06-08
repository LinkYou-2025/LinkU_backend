create table alarms
(
    alarm_id   bigint auto_increment
        primary key,
    created_at datetime(6)  null,
    updated_at datetime(6)  null,
    alarm_type varchar(100) not null,
    body       tinytext     not null,
    target_id  bigint       not null,
    title      varchar(100) not null
);

create table curation_section_info
(
    id             bigint auto_increment
        primary key,
    created_at     datetime(6)  null,
    updated_at     datetime(6)  null,
    description    text         null,
    image_url      text         null,
    base_month     varchar(7)   not null,
    section_number int          not null,
    title          varchar(255) not null,
    constraint uq_curation_section
        unique (base_month, section_number)
);

create table domain
(
    domain_id      bigint auto_increment
        primary key,
    crawl_strategy varchar(50)  null,
    domain_tail    varchar(255) not null,
    image_url      text         null,
    name           varchar(100) not null
);

create table emotion
(
    emotion_id bigint auto_increment
        primary key,
    name       varchar(100) not null
);

create table curation_ment
(
    curation_ment_id bigint auto_increment
        primary key,
    footer_text      text   not null,
    header_text      text   not null,
    emotion_id       bigint not null,
    constraint FKrrgvccdyx159ub0ccvrkafrdp
        foreign key (emotion_id) references emotion (emotion_id)
);

create table fcolor
(
    fcolor_id   bigint auto_increment
        primary key,
    color_code1 varchar(20) not null,
    color_code2 varchar(20) not null,
    color_code3 varchar(20) not null,
    color_code4 varchar(20) not null,
    color_name  varchar(50) not null
);

create table category
(
    category_id   bigint auto_increment
        primary key,
    category_name varchar(100) not null,
    fcolor_id     bigint       not null,
    constraint FKc8fmnv6oqsj56sucecgw0p0ph
        foreign key (fcolor_id) references fcolor (fcolor_id)
);


create table folder
(
    folder_id        bigint auto_increment
        primary key,
    created_at       datetime(6)  null,
    updated_at       datetime(6)  null,
    folder_name      varchar(255) not null,
    category_id      bigint       not null,
    parent_folder_id bigint       null,
    constraint FK325oi9nqny99x3svwu4xja11b
        foreign key (category_id) references category (category_id),
    constraint FK57g7veis1gp5wn3g0mp0x57pl
        foreign key (parent_folder_id) references folder (folder_id)
);

create table job
(
    job_id bigint auto_increment
        primary key,
    name   varchar(100) not null
);

create table situation
(
    situation_id bigint auto_increment
        primary key,
    name         varchar(100) not null
);

create table ai_article
(
    ai_article_id  bigint auto_increment
        primary key,
    created_at     datetime(6)  null,
    updated_at     datetime(6)  null,
    ai_category_id bigint       null,
    ai_feeling_id  bigint       null,
    img_url        text         null,
    keyword        text         null,
    summary        varchar(255) not null,
    title          varchar(255) not null,
    linku_id       bigint       not null,
    situation_id   bigint       not null,
    constraint UK_ljjpq2dyny9m5gtecqmf2loda
        unique (linku_id),
    constraint FKfva0df5p4blnk9ho3hl8m1x27
        foreign key (situation_id) references situation (situation_id)
);

create table linku
(
    linku_id         bigint auto_increment
        primary key,
    created_at       datetime(6) null,
    updated_at       datetime(6) null,
    linku            text        not null,
    title            text        not null,
    total_view_count bigint      not null,
    ai_article_id    bigint      null,
    category_id      bigint      not null,
    domain_id        bigint      not null,
    constraint FK16f5cbdjeu1sqj9rrgtdtc260
        foreign key (category_id) references category (category_id),
    constraint FK3fxlvilewjb4kddorcv7ava5
        foreign key (domain_id) references domain (domain_id),
    constraint FKo8x0ac36fbx57xcfuys3jtvhi
        foreign key (ai_article_id) references ai_article (ai_article_id)
);

alter table ai_article
    add constraint FKn0qhgeap8lpb8akqn0nttf8d0
        foreign key (linku_id) references linku (linku_id);

create table situation_category
(
    id           bigint auto_increment
        primary key,
    category_id  bigint not null,
    situation_id bigint not null,
    constraint FK7ohvi0orgx8gs82po59vfeft8
        foreign key (situation_id) references situation (situation_id),
    constraint FKoeb7bdsvdyqn1a3u1awwd4usw
        foreign key (category_id) references category (category_id)
);

create table situation_job
(
    situation_job_id bigint auto_increment
        primary key,
    job_id           bigint not null,
    situation_id     bigint not null,
    constraint FK1y1ex7jxw3tdb1w279tr2l64f
        foreign key (situation_id) references situation (situation_id),
    constraint FKiyxgx7gf7er1cv329tnnh4vjh
        foreign key (job_id) references job (job_id)
);

create table users
(
    user_id        bigint auto_increment
        primary key,
    created_at     datetime(6)  null,
    updated_at     datetime(6)  null,
    deleted_reason text         null,
    gender         varchar(255) null,
    inactive_date  datetime(6)  null,
    nick_name      varchar(255) not null,
    password       varchar(255) not null,
    role           varchar(255) null,
    status         varchar(255) null,
    job_id         bigint       null,
    constraint UK_pl4047a5k5enw6up4sjs8lyut
        unique (nick_name),
    constraint FK7erlbfqh4er9f21ngd9nvylwe
        foreign key (job_id) references job (job_id)
);

create table alarm_setting
(
    alarm_setting_id  bigint auto_increment
        primary key,
    alarm_all_enabled bit    not null,
    curation_enabled  bit    not null,
    folder_enabled    bit    not null,
    link_enabled      bit    not null,
    notice_enabled    bit    not null,
    user_id           bigint not null,
    constraint UK_rbpy1243tci779nxsdicrwpko
        unique (user_id),
    constraint FKkb2xi0karg4ewki4hsiseix
        foreign key (user_id) references users (user_id)
);

create table auth_account
(
    social_account_id bigint auto_increment
        primary key,
    created_at        datetime(6)  null,
    updated_at        datetime(6)  null,
    email             varchar(255) not null,
    external_id       text         not null,
    profile_image     text         null,
    provider          varchar(50)  not null,
    social_token      text         null,
    user_id           bigint       not null,
    constraint FKodq2wghn30g9soimyc2hlesyy
        foreign key (user_id) references users (user_id)
);

create table curation
(
    curation_id bigint auto_increment
        primary key,
    created_at  datetime(6) null,
    updated_at  datetime(6) null,
    base_month  varchar(7)  not null,
    footer_ment text        null,
    header_ment text        null,
    user_id     bigint      not null,
    constraint FKjy12cq7kg7pc5wc27vxexhb80
        foreign key (user_id) references users (user_id)
);

create table curation_like
(
    curation_like_id bigint auto_increment
        primary key,
    created_at       datetime(6) null,
    updated_at       datetime(6) null,
    curation_id      bigint      not null,
    user_id          bigint      not null,
    constraint FK3b1crj0tvpf9kk41v9508n49b
        foreign key (user_id) references users (user_id),
    constraint FKteldaniufcrec9rkuxkh48s3f
        foreign key (curation_id) references curation (curation_id)
);

create table folder_share_link
(
    folder_share_link_id bigint auto_increment
        primary key,
    created_at           datetime(6)  null,
    updated_at           datetime(6)  null,
    expires_at           datetime(6)  not null,
    is_active            bit          not null,
    permission_type      varchar(255) not null,
    token                varchar(64)  not null,
    creator_id           bigint       not null,
    folder_id            bigint       not null,
    constraint UK_lvwba0r0capwte08yoypecb8m
        unique (token),
    constraint FK7lt90xg8l17oh39eawo2wegpw
        foreign key (folder_id) references folder (folder_id),
    constraint FKr2dfugjfl1nqfnvyt1j2jgyf3
        foreign key (creator_id) references users (user_id)
);

create table interests
(
    id          bigint        auto_increment
        primary key,
    interest    varchar(255) not null,
    selected_at datetime(6)  not null,
    user_id     bigint       not null,
    constraint FKq9kr60l7n7h3yf82s44rkoe4g
        foreign key (user_id) references users (user_id)
);

create table keyword_monthly_count
(
    keyword_monthly_count_id bigint auto_increment
        primary key,
    base_month               varchar(7)  not null,
    count                    int         not null,
    ref_id                   bigint      not null,
    type                     varchar(10) not null,
    user_id                  bigint      not null,
    constraint uq_keyword_monthly
        unique (user_id, type, ref_id, base_month),
    constraint FKh9u4ar9ye83lqdrfcilany1sn
        foreign key (user_id) references users (user_id)
);

create table purposes
(
    id          bigint       auto_increment
        primary key,
    purpose     varchar(255) not null,
    selected_at datetime(6)  not null,
    user_id     bigint       not null,
    constraint FKmxdki4pg08vyfr90wovf57y47
        foreign key (user_id) references users (user_id)
);

create table terms_agreement
(
    terms_agreement_id bigint auto_increment
        primary key,
    created_at         datetime(6) null,
    updated_at         datetime(6) null,
    agreed_at          datetime(6) not null,
    is_agreed          bit         not null,
    is_required        bit         not null,
    terms_type         varchar(50) not null,
    terms_version      varchar(10) not null,
    user_id            bigint      not null,
    constraint uk_terms_agreements_user_type
        unique (user_id, terms_type),
    constraint FKj2lqrutslqkrxai3ww5h9w9q9
        foreign key (user_id) references users (user_id)
);

create table user_alarm
(
    user_alarm_id bigint auto_increment
        primary key,
    created_at    datetime(6) null,
    updated_at    datetime(6) null,
    delivered_at  datetime(6) not null,
    is_read       bit         not null,
    read_at       datetime(6) null,
    alarm_id      bigint      not null,
    user_id       bigint      not null,
    constraint uq_user_alarm_user_alarm
        unique (user_id, alarm_id),
    constraint FK56hrhm2hboyuciaw7bu6knwx3
        foreign key (user_id) references users (user_id),
    constraint FKfr2i1hg12vpw1tugptojsak0x
        foreign key (alarm_id) references alarms (alarm_id)
);

create table user_fcm_token
(
    user_fcm_token_id bigint auto_increment
        primary key,
    created_at        datetime(6) null,
    updated_at        datetime(6) null,
    expires_at        datetime(6) null,
    fcm_token         text        not null,
    is_active         bit         null,
    last_used_at      datetime(6) null,
    user_id           bigint      not null,
    constraint FKqpnhn7yuj3e351r3x9og639m8
        foreign key (user_id) references users (user_id)
);

create table users_category_color
(
    users_category_color_id bigint auto_increment
        primary key,
    category_id             bigint not null,
    fcolor_id               bigint not null,
    user_id                 bigint not null,
    constraint FK5xhuy9l7vmsukyaksh2dji7h3
        foreign key (user_id) references users (user_id),
    constraint FK602fiiov3hqijqoa3k9hnlhae
        foreign key (category_id) references category (category_id),
    constraint FKg0ju3bje329n7hy4o93lehajr
        foreign key (fcolor_id) references fcolor (fcolor_id)
);

create table users_folder
(
    users_folder_id bigint auto_increment
        primary key,
    created_at      datetime(6)  null,
    updated_at      datetime(6)  null,
    is_bookmarked   bit          null,
    permission_type varchar(255) not null,
    folder_id       bigint       not null,
    user_id         bigint       not null,
    constraint FKf9ut8x0o1r0n79mt31xh06jnv
        foreign key (user_id) references users (user_id),
    constraint FKnjhgy813ymtl1d2dvh911cp8y
        foreign key (folder_id) references folder (folder_id)
);

create table users_linku
(
    user_linku_id  bigint auto_increment
        primary key,
    created_at     datetime(6)  null,
    updated_at     datetime(6)  null,
    is_ai_exist    bit          not null,
    image_url      text         null,
    last_viewed_at datetime(6)  null,
    memo           varchar(255) null,
    view_count     int          not null,
    emotion_id     bigint       not null,
    linku_id       bigint       not null,
    user_id        bigint       not null,
    constraint FK72es4k8q3ognjq84mk4i4iodp
        foreign key (linku_id) references linku (linku_id),
    constraint FKf0ghvxxt5jdp9y5vsfkp0usxe
        foreign key (emotion_id) references emotion (emotion_id),
    constraint FKmk9cflmp54tf9i2sx1qdd3pf4
        foreign key (user_id) references users (user_id)
);

create table curation_linku
(
    curation_linku_id bigint auto_increment
        primary key,
    image_url         varchar(1024) null,
    title             varchar(255)  null,
    type              varchar(255)  not null,
    url               text          null,
    url_normalized    varchar(2048) null,
    curation_id       bigint        not null,
    user_linku_id     bigint        null,
    constraint FK6onak023fg90k5t7g3shfbp42
        foreign key (curation_id) references curation (curation_id),
    constraint FKeq6oyxtatf8xqr4fd1glddq3e
        foreign key (user_linku_id) references users_linku (user_linku_id)
);

create table linku_folder
(
    linku_folder_id bigint auto_increment
        primary key,
    folder_id       bigint not null,
    user_linku_id   bigint not null,
    constraint FK4sqgn921md2ysu00g0ekonkau
        foreign key (user_linku_id) references users_linku (user_linku_id),
    constraint FKe2gx1ngf7g7spe5syr0txbihl
        foreign key (folder_id) references folder (folder_id)
);

