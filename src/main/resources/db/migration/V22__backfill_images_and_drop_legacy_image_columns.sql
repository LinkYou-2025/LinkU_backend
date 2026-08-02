-- 이미지 저장 방식 변경 (Object Key-only 저장) - 2단계: 기존 데이터 백필 + 옛 컬럼 제거
-- V21에서 준비한 images 테이블/FK로 기존 데이터를 옮긴 뒤, 더 이상 쓰지 않는 옛 컬럼을 지운다.
-- 이 백필은 실제 운영 데이터(도메인/링크/사용자링크 이미지)를 옮기는 작업이라 운영 DB에서도
-- 반드시 실행돼야 하므로 db/migration에 둔다 (application-prod.yml의 flyway locations는
-- db/migration만 스캔하고 db/seed는 포함하지 않는다).
--
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
