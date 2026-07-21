-- alarms.alarm_type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'alarms_alarm_type_check') THEN
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
    END IF;
END $$;

-- domains.crawl_strategy
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'domains_crawl_strategy_check') THEN
        ALTER TABLE domains
            ADD CONSTRAINT domains_crawl_strategy_check
                CHECK (crawl_strategy = ANY (ARRAY [
                    'IFRAME',
                    'BODY',
                    'DEFAULT'
                    ]::varchar[]));
    END IF;
END $$;

-- auth_accounts.provider
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_accounts_provider_check') THEN
        ALTER TABLE auth_accounts
            ADD CONSTRAINT auth_accounts_provider_check
                CHECK (provider = ANY (ARRAY [
                    'GENERAL',
                    'KAKAO',
                    'GOOGLE',
                    'NAVER'
                    ]::varchar[]));
    END IF;
END $$;

-- terms_agreements.terms_type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'terms_agreements_terms_type_check') THEN
        ALTER TABLE terms_agreements
            ADD CONSTRAINT terms_agreements_terms_type_check
                CHECK (terms_type = ANY (ARRAY [
                    'TERMS_OF_USE',
                    'PRIVACY_POLICY',
                    'MARKETING'
                    ]::varchar[]));
    END IF;
END $$;

-- folder_share_links.permission_type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'folder_share_links_permission_type_check') THEN
        ALTER TABLE folder_share_links
            ADD CONSTRAINT folder_share_links_permission_type_check
                CHECK (permission_type = ANY (ARRAY [
                    'VIEWER',
                    'WRITER',
                    'OWNER',
                    'NONE'
                    ]::varchar[]));
    END IF;
END $$;

-- users_folders.permission_type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_folders_permission_type_check') THEN
        ALTER TABLE users_folders
            ADD CONSTRAINT users_folders_permission_type_check
                CHECK (permission_type = ANY (ARRAY [
                    'VIEWER',
                    'WRITER',
                    'OWNER',
                    'NONE'
                    ]::varchar[]));
    END IF;
END $$;

-- keyword_monthly_counts.type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'keyword_monthly_counts_type_check') THEN
        ALTER TABLE keyword_monthly_counts
            ADD CONSTRAINT keyword_monthly_counts_type_check
                CHECK (type = ANY (ARRAY [
                    'EMOTION',
                    'SITUATION'
                    ]::varchar[]));
    END IF;
END $$;

-- curation_linkus.type
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'curation_linkus_type_check') THEN
        ALTER TABLE curation_linkus
            ADD CONSTRAINT curation_linkus_type_check
                CHECK (type = ANY (ARRAY [
                    'RECOMMENDED',
                    'EXTERNAL'
                    ]::varchar[]));
    END IF;
END $$;
