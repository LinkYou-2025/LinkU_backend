-- curation_linkus.type: RECOMMENDED -> INTERNAL (명칭을 EXTERNAL과 대칭되도록 변경)
ALTER TABLE curation_linkus DROP CONSTRAINT IF EXISTS curation_linkus_type_check;

UPDATE curation_linkus SET type = 'INTERNAL' WHERE type = 'RECOMMENDED';

ALTER TABLE curation_linkus
    ADD CONSTRAINT curation_linkus_type_check
        CHECK (type = ANY (ARRAY [
            'INTERNAL',
            'EXTERNAL'
            ]::varchar[]));
