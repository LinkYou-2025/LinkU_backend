-- url_normalized: 정규화된 url을 별도 저장하지 않고 url 컬럼에 정규화된 값을 바로 저장하기로 변경
ALTER TABLE curation_linkus DROP COLUMN IF EXISTS url_normalized;
