-- 이미지 저장 방식 변경 (Object Key-only 저장)
-- DB에 CloudFront URL 전체를 저장하는 대신, S3 object key(상대 경로) 또는 외부 이미지 URL만
-- 별도의 images 테이블에서 source_type으로 구분해 관리한다.
-- domains.image_url / linkus.img_url / users_linkus.image_url 을 각각 image_id FK로 대체한다.

create table images
(
    id          bigserial   primary key,
    created_at  timestamp(6),
    updated_at  timestamp(6),
    source_type varchar(20) not null,
    location    text        not null,
    constraint images_source_type_check check (source_type in ('S3', 'EXTERNAL'))
);

alter table domains
    add column image_id bigint;
alter table domains
    add constraint fk_domains_image
        foreign key (image_id) references images (id);

alter table linkus
    add column image_id bigint;
alter table linkus
    add constraint fk_linkus_image
        foreign key (image_id) references images (id);

alter table users_linkus
    add column image_id bigint;
alter table users_linkus
    add constraint fk_users_linkus_image
        foreign key (image_id) references images (id);

-- 기존 데이터 백필
-- domains / users_linkus: S3 출처 이미지. 값이 CloudFront 전체 URL이면 도메인 접두사를 제거하고,
--   이미 "/"로 시작하는 상대 경로(신규 시드 데이터)면 그대로 사용해 "/"로 시작하는 object key로 통일한다.
-- linkus: 크롤링으로 얻은 외부 이미지 URL(EXTERNAL 출처)이므로 값을 그대로 옮긴다.
do $$
declare
    r record;
    new_image_id bigint;
begin
    for r in select domain_id, image_url from domains where image_url is not null loop
        insert into images (source_type, location, created_at, updated_at)
        values (
            'S3',
            case
                when r.image_url ~ '^https?://' then '/' || regexp_replace(r.image_url, '^https?://[^/]+/?', '')
                when r.image_url like '/%' then r.image_url
                else '/' || r.image_url
            end,
            now(), now()
        )
        returning id into new_image_id;

        update domains set image_id = new_image_id where domain_id = r.domain_id;
    end loop;

    for r in select linku_id, img_url from linkus where img_url is not null loop
        insert into images (source_type, location, created_at, updated_at)
        values ('EXTERNAL', r.img_url, now(), now())
        returning id into new_image_id;

        update linkus set image_id = new_image_id where linku_id = r.linku_id;
    end loop;

    for r in select user_linku_id, image_url from users_linkus where image_url is not null loop
        insert into images (source_type, location, created_at, updated_at)
        values (
            'S3',
            case
                when r.image_url ~ '^https?://' then '/' || regexp_replace(r.image_url, '^https?://[^/]+/?', '')
                when r.image_url like '/%' then r.image_url
                else '/' || r.image_url
            end,
            now(), now()
        )
        returning id into new_image_id;

        update users_linkus set image_id = new_image_id where user_linku_id = r.user_linku_id;
    end loop;
end $$;

alter table domains
    drop column image_url;
alter table linkus
    drop column img_url;
alter table users_linkus
    drop column image_url;
