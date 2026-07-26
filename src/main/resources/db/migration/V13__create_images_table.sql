-- 이미지 저장 방식 변경 (Object Key-only 저장) - 1단계: 스키마 준비
-- DB에 CloudFront URL 전체를 저장하는 대신, S3 object key(상대 경로) 또는 외부 이미지 URL만
-- 별도의 images 테이블에서 source_type으로 구분해 관리한다.
-- domains.image_url / linkus.img_url / users_linkus.image_url 을 각각 image_id FK로 대체하기 위한
-- 테이블/컬럼 준비만 여기서 하고, 기존 데이터 백필과 옛 컬럼 제거는 V14에서 진행한다.
-- (기존 컬럼 값을 옮기기 전에 지워버리면 안 되므로 반드시 V14와 분리한다)

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
