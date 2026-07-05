-- alarms.alarm_type
ALTER TABLE alarms
    ADD CONSTRAINT alarms_alarm_type_check
        CHECK (alarm_type = ANY (ARRAY [
            'LINK_SUMMARY_COMPLETE',
            'FOLDER_DELETED',
            'FOLDER_PERMISSION_CHANGED',
            'CURATION_UPDATED',
            'ANNOUNCEMENT_UPDATE',
            'ANNOUNCEMENT_ERROR'
            ]::varchar[]));

-- domains.crawl_strategy
ALTER TABLE domains
    ADD CONSTRAINT domains_crawl_strategy_check
        CHECK (crawl_strategy = ANY (ARRAY [
            'IFRAME',
            'BODY',
            'DEFAULT'
            ]::varchar[]));

-- auth_accounts.provider
ALTER TABLE auth_accounts
    ADD CONSTRAINT auth_accounts_provider_check
        CHECK (provider = ANY (ARRAY [
            'GENERAL',
            'KAKAO',
            'GOOGLE',
            'NAVER'
            ]::varchar[]));

-- terms_agreements.terms_type
ALTER TABLE terms_agreements
    ADD CONSTRAINT terms_agreements_terms_type_check
        CHECK (terms_type = ANY (ARRAY [
            'TERMS_OF_USE',
            'PRIVACY_POLICY',
            'MARKETING'
            ]::varchar[]));

-- folder_share_links.permission_type
ALTER TABLE folder_share_links
    ADD CONSTRAINT folder_share_links_permission_type_check
        CHECK (permission_type = ANY (ARRAY [
            'VIEWER',
            'WRITER',
            'OWNER',
            'NONE'
            ]::varchar[]));

-- users_folders.permission_type
ALTER TABLE users_folders
    ADD CONSTRAINT users_folders_permission_type_check
        CHECK (permission_type = ANY (ARRAY [
            'VIEWER',
            'WRITER',
            'OWNER',
            'NONE'
            ]::varchar[]));

-- keyword_monthly_counts.type
ALTER TABLE keyword_monthly_counts
    ADD CONSTRAINT keyword_monthly_counts_type_check
        CHECK (type = ANY (ARRAY [
            'EMOTION',
            'SITUATION'
            ]::varchar[]));

-- curation_linkus.type
ALTER TABLE curation_linkus
    ADD CONSTRAINT curation_linkus_type_check
        CHECK (type = ANY (ARRAY [
            'RECOMMENDED',
            'EXTERNAL'
            ]::varchar[]));
