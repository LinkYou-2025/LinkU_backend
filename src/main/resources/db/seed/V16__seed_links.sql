-- 홈화면 추천 로직(TextMatch/KeywordMatch/PersonalEngagement 등) 검증용 테스트 링크 데이터.
-- 출처: 사용자 노션 링크 데베 CSV export. 크롤링 실패(본문추출불가/URL 없음/사이트 nav만 긁힘) 44건 제외 후
-- 남은 168건을 카테고리 기준 13개 테스트 계정(V14와 별개)에 10~20개씩 분배했다.
-- text 컬럼(원문 스크랩)은 실제 Gemini 요약 파이프라인 대신 수작업으로 요약해 ai_articles.summary에 넣었다.
-- emotion/situation은 실제 유저가 고른 값이 아니라 콘텐츠를 보고 유추해 넣은 것이므로 emotion_ai/situation_ai=true로 표시한다.
-- domain은 별도 매핑 없이 전부 DEFAULT_DOMAIN_ID(=1, LinkuCreateService 기준)를 사용한다.


-- ============ 2. linku / ai_articles / users_linkus ============
-- #2 (어학) -> seed_lang_learner | blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900001, 1, 1, 'https://blog.naver.com/moki_allrecords/223527506604', 'OPIC 오픽 AL 독학 하루 전 벼락치기 꿀팁후기(서울동부 자격검정센터, 강서CBT센터)', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900001, 900001, '오픽 AL을 독학으로 준비한 사람이 이전 응시 이력, 공부 방법, 시험 후기와 응시장 추천까지 하루 전 벼락치기 기준으로 정리한 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900001, 2, 10, true, true, true, 0, 'OPIC 오픽 AL 독학 하루 전 벼락치기 꿀팁후기(서울동부 자격검정센터, 강서CBT센터)', now(), now());

-- #3 (어학) -> seed_lang_learner | kimdee.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900002, 1, 1, 'https://kimdee.tistory.com/entry/오픽OPIC-독학으로-IH-딴-후기', '[오픽OPIC] 영어공부 안 했는데 IH 단번에 취득한 방법, 오픽에도 전략이 있다니 👀', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900002, 900002, '6년 만에 다시 본 오픽에서 별도 스크립트 암기 없이 전략만으로 IH를 받은 경험과 그 노하우를 담은 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900002, 2, 16, true, true, true, 0, '[오픽OPIC] 영어공부 안 했는데 IH 단번에 취득한 방법, 오픽에도 전략이 있다니 👀', now(), now());

-- #4 (어학) -> seed_lang_learner | community.linkareer.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900003, 1, 1, 'https://community.linkareer.com/honeytips/2320116', '[오픽 꿀팁] 오픽 AL 받은 취준생이 알려주는 오픽 팁들', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900003, 900003, '취업 준비생이 서베이 선택 요령 등 오픽 시험을 준비하며 얻은 실전 팁들을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900003, 2, 10, true, true, true, 0, '[오픽 꿀팁] 오픽 AL 받은 취준생이 알려주는 오픽 팁들', now(), now());

-- #5 (어학) -> seed_lang_learner | studyingpark.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900004, 1, 1, 'https://studyingpark.tistory.com/65', '미드에 나온 쓸만한 영어문장 50개 (#2)', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900004, 900004, '가십걸 등 미국 드라마 대사에서 뽑은 실전 영어 문장 50개를 정리한 시리즈의 두 번째 편.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900004, 2, 16, true, true, true, 0, '미드에 나온 쓸만한 영어문장 50개 (#2)', now(), now());

-- #6 (어학) -> seed_lang_learner | www.amazingtalker.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900005, 1, 1, 'https://www.amazingtalker.co.kr/blog/ko/kr-en/46339/', '재밌는 영어공부 위해 미드 영드 15작품 추천! (ft.영어 쉐도잉)', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900005, 900005, '미드·영드로 쉐도잉하며 재미있게 영어를 공부하는 방법과 추천 작품 15편을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900005, 2, 10, true, true, true, 0, '재밌는 영어공부 위해 미드 영드 15작품 추천! (ft.영어 쉐도잉)', now(), now());

-- #7 (어학) -> seed_lang_learner | vancouver-greenthumb.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900006, 1, 1, 'https://vancouver-greenthumb.tistory.com/entry/미드에서-배우는-일상영어-50-문장', '[영어공부] 미드에서 배우는 일상영어: 50 문장', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900006, 900006, '미국 드라마 속 일상 영어 표현 50문장을 정리해 회화 감각을 키우도록 돕는 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900006, 2, 16, true, true, true, 0, '[영어공부] 미드에서 배우는 일상영어: 50 문장', now(), now());

-- #8 (어학) -> seed_lang_learner | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900007, 1, 1, 'https://brunch.co.kr/@dailynews/687', '영어공부하기 좋은 미드 추천 10', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900007, 900007, '영어 공부하기 좋은 미국 드라마 10편을 추천하며 각각의 특징을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900007, 2, 10, true, true, true, 0, '영어공부하기 좋은 미드 추천 10', now(), now());

-- #9 (어학) -> seed_lang_learner | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900008, 1, 1, 'https://m.blog.naver.com/PostView.naver?blogId=gpdnjs1641&logNo=222881150346&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '2022년 핫한 영어 슬랭 top 5 공부해요', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900008, 900008, 'SNS에서 외국인과 소통하며 알게 된 2022년 유행 영어 슬랭 다섯 가지를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900008, 2, 16, true, true, true, 0, '2022년 핫한 영어 슬랭 top 5 공부해요', now(), now());

-- #10 (어학) -> seed_lang_learner | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900009, 1, 1, 'https://m.blog.naver.com/isabelsoyoung/222522654649', '스페인어 독학 나만의 꿀팁 공개', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900009, 900009, '스페인어 전공자가 스페인어 독학 노하우와 개인적인 공부 꿀팁을 공개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900009, 2, 10, true, true, true, 0, '스페인어 독학 나만의 꿀팁 공개', now(), now());

-- #11 (어학) -> seed_lang_learner | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900010, 1, 1, 'https://brunch.co.kr/@chlngers/5', '중국인도 인정한 중국어 공부법', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900010, 900010, '중국어를 독학으로 익혀 중국인도 인정할 정도의 실력을 갖추게 된 경험담.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900010, 2, 16, true, true, true, 0, '중국인도 인정한 중국어 공부법', now(), now());

-- #15 (어학) -> seed_lang_learner | storytender.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900011, 1, 1, 'https://storytender.tistory.com/79', '듀오링고 영어 테스트 첫 시험에서 130점 받은 후기 (3일 벼락치기 공부법, 토플 80점대, DET 전망, 시험일정)', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900011, 900011, '3일 벼락치기 준비로 듀오링고 영어 테스트에서 130점을 받은 후기와 토플 병행 준비 전략.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900011, 2, 10, true, true, true, 0, '듀오링고 영어 테스트 첫 시험에서 130점 받은 후기 (3일 벼락치기 공부법, 토플 80점대, DET 전망, 시험일정)', now(), now());

-- #16 (어학) -> seed_lang_learner | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900012, 1, 1, 'https://m.blog.naver.com/dlqufrltnfwk/223425780480', '토익 990점 후기 및 영어공부 얘기', 0, 2, 16, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900012, 900012, '오랜만에 영어 공부를 재개해 토익 990점 만점을 받은 후기와 그 과정에서의 학습 방법.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900012, 2, 16, true, true, true, 0, '토익 990점 후기 및 영어공부 얘기', now(), now());

-- #17 (어학) -> seed_lang_learner | bluejayblog.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900013, 1, 1, 'https://bluejayblog.tistory.com/entry/토익-10일-준비해서-990점-만점-받기-독학-팁-모음-토익-990점', '토익 10일 준비해서 990점 만점 받기 [독학, 팁 모음, 토익 990점]', 0, 2, 10, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900013, 900013, '10일 동안 독학으로 준비해 토익 990점 만점을 받은 일정표와 준비 노하우 정리.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (101, 900013, 2, 10, true, true, true, 0, '토익 10일 준비해서 990점 만점 받기 [독학, 팁 모음, 토익 990점]', now(), now());

-- #19 (뉴스) -> seed_media_culture | story.pay.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900014, 2, 1, 'https://story.pay.naver.com/content/1186_3_C1', '지난주 폭락 딛고 다시 오른 코스피, 5,000까지 오를 수 있을까?', 0, 5, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900014, 900014, '지난주 급락 후 반등한 코스피 시황과 5000선 돌파 가능성을 짚은 국내 증시 요약 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900014, 5, 18, true, true, true, 0, '지난주 폭락 딛고 다시 오른 코스피, 5,000까지 오를 수 있을까?', now(), now());

-- #20 (뉴스) -> seed_media_culture | www.digitaltoday.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900015, 2, 1, 'https://www.digitaltoday.co.kr/news/articleView.html?idxno=578296', '암호화폐 시총 4조달러 돌파…알트코인 불장 언제쯤?', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900015, 900015, '암호화폐 시가총액이 4조달러를 돌파한 가운데 알트코인 강세장 시점을 전망한 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900015, 2, 23, true, true, true, 0, '암호화폐 시총 4조달러 돌파…알트코인 불장 언제쯤?', now(), now());

-- #22 (뉴스) -> seed_media_culture | www.munhwa.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900016, 2, 1, 'https://www.munhwa.com/article/11525924', '‘12만3491달러’ 비트코인 또 신고가… 시가총액 2위 이더리움도 경신 눈앞', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900016, 900016, '비트코인이 또 한 번 신고가를 경신하고 이더리움도 추격에 나선 가상자산 시황 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900016, 2, 18, true, true, true, 0, '‘12만3491달러’ 비트코인 또 신고가… 시가총액 2위 이더리움도 경신 눈앞', now(), now());

-- #23 (뉴스) -> seed_media_culture | www.korea.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900017, 2, 1, 'https://www.korea.kr/news/policyNewsView.do?newsId=148947620', '광복 80주년 기념곡 ''꺼지지 않는 빛'' 공개…''케데헌'' 이재 작곡', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900017, 900017, '광복 80주년 기념곡 ''꺼지지 않는 빛''의 공개와 작곡가 이재(EJAE)의 참여 소식을 다룬 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900017, 2, 23, true, true, true, 0, '광복 80주년 기념곡 ''꺼지지 않는 빛'' 공개…''케데헌'' 이재 작곡', now(), now());

-- #24 (뉴스) -> seed_media_culture | www.donga.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900018, 2, 1, 'https://www.donga.com/news/Culture/article/all/20250814/132188036/1', '‘바이올리니스트 거장’ 샤함·앤서니, 부부의 하모니로 한국 첫 무대 선다', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900018, 900018, '바이올리니스트 샤함과 앤서니 부부가 한국에서 처음으로 무대에 오른다는 공연 소식.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900018, 2, 18, true, true, true, 0, '‘바이올리니스트 거장’ 샤함·앤서니, 부부의 하모니로 한국 첫 무대 선다', now(), now());

-- #26 (뉴스) -> seed_media_culture | www.hankyung.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900019, 2, 1, 'https://www.hankyung.com/article/2025081499427', '속보] ''유류세 인하'' 10월 말까지 두달 더 연장…휘발유 10%·경유 15%', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900019, 900019, '유류세 인하 조치가 10월 말까지 두 달 더 연장된다는 정부 발표 속보.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900019, 2, 23, true, true, true, 0, '속보] ''유류세 인하'' 10월 말까지 두달 더 연장…휘발유 10%·경유 15%', now(), now());

-- #27 (뉴스) -> seed_media_culture | www.fortunekorea.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900020, 2, 1, 'https://www.fortunekorea.co.kr/news/articleView.html?idxno=49224', '[C.C] 사모펀드, 구조조정, 그리고 책임…JKL파트너스의 24년', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900020, 900020, 'JKL파트너스 대표 인터뷰를 통해 짚어본 사모펀드 업계의 구조조정과 24년간의 역사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900020, 2, 18, true, true, true, 0, '[C.C] 사모펀드, 구조조정, 그리고 책임…JKL파트너스의 24년', now(), now());

-- #28 (뉴스) -> seed_media_culture | www.koreaherald.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900021, 2, 1, 'https://www.koreaherald.com/article/10554365', 'From ashes of war to arsenal of world: South Korea’s defense industry boom', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900021, 900021, '한국 방위산업이 전쟁의 폐허에서 세계적 수출 산업으로 성장한 과정을 다룬 외신 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900021, 2, 23, true, true, true, 0, 'From ashes of war to arsenal of world: South Korea’s defense industry boom', now(), now());

-- #29 (공부법) -> seed_studymethod | community.linkareer.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900022, 3, 1, 'https://community.linkareer.com/board_FJnt73/267021', '회계 자격증 비교해보기 (난도, 프로그램 사용, 응시료 등)', 0, 2, 2, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900022, 900022, '회계 관련 자격증들을 난이도, 프로그램 활용, 응시료 등 기준으로 비교한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900022, 2, 2, true, true, true, 0, '회계 자격증 비교해보기 (난도, 프로그램 사용, 응시료 등)', now(), now());

-- #30 (공부법) -> seed_studymethod | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900023, 3, 1, 'https://m.blog.naver.com/PostView.naver?blogId=kiwiji&logNo=223526513545&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', 'SQLD시험 & 경영정보시각화능력 필기 합격 공부방법', 0, 2, 4, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900023, 900023, 'SQLD와 경영정보시각화능력 필기를 함께 준비해 합격한 공부 방법을 정리한 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900023, 2, 4, true, true, true, 0, 'SQLD시험 & 경영정보시각화능력 필기 합격 공부방법', now(), now());

-- #31 (공부법) -> seed_studymethod | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900024, 3, 1, 'https://m.blog.naver.com/PostView.naver?blogId=jenny_1000&logNo=223401388719&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '재경관리사 시험 후기 (feat. 4수 졸업)', 0, 2, 2, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900024, 900024, '네 번의 도전 끝에 재경관리사 시험에 합격한 경험담과 소회.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900024, 2, 2, true, true, true, 0, '재경관리사 시험 후기 (feat. 4수 졸업)', now(), now());

-- #32 (공부법) -> seed_studymethod | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900025, 3, 1, 'https://m.blog.naver.com/PostView.naver?blogId=amy2458&logNo=223142660976&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '[테셋/S급] 2주 독학, 테셋 S급 합격 후기_무료자료, 강의, 공부법추천(2023 특시)', 0, 2, 4, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900025, 900025, '2주 독학만으로 테셋 시험에서 S급을 받은 합격 후기와 무료 자료·강의 추천.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900025, 2, 4, true, true, true, 0, '[테셋/S급] 2주 독학, 테셋 S급 합격 후기_무료자료, 강의, 공부법추천(2023 특시)', now(), now());

-- #33 (공부법) -> seed_studymethod | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900026, 3, 1, 'https://m.blog.naver.com/PostView.naver?blogId=jenny_1000&logNo=223379210977&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '제37회 투자자산운용사 시험 후기 + 독학 공부 과정', 0, 2, 2, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900026, 900026, '투자자산운용사 시험을 준비하며 겪은 독학 과정과 합격 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900026, 2, 2, true, true, true, 0, '제37회 투자자산운용사 시험 후기 + 독학 공부 과정', now(), now());

-- #34 (공부법) -> seed_studymethod | www.megastudy.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900027, 3, 1, 'https://www.megastudy.net/campaign/study/snote_view.asp?mOne=study3&idx=1116856&rc_mod=sub&rc_cd=snote', '생명과학1 만점 공부법', 0, 2, 4, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900027, 900027, '생명과학1에서 만점을 받기 위한 개념·기출 학습 전략을 정리한 공부법 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900027, 2, 4, true, true, true, 0, '생명과학1 만점 공부법', now(), now());

-- #35 (공부법) -> seed_studymethod | www.ebsi.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900028, 3, 1, 'https://www.ebsi.co.kr/ebs/ent/enta/retrieveMyGlUnivLearnEntAnalysisView.ebs', '학습전략 칼럼', 0, 2, 2, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900028, 900028, '효과적인 학습 전략을 소개하는 교육 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900028, 2, 2, true, true, true, 0, '학습전략 칼럼', now(), now());

-- #37 (공부법) -> seed_studymethod | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900029, 3, 1, 'https://m.blog.naver.com/hwko0916/222797039144', '12시간의 치타 공부법 후기', 0, 2, 4, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900029, 900029, '스터디카페에서 12시간을 몰아 공부한 이른바 ''치타 공부법'' 실천 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900029, 2, 4, true, true, true, 0, '12시간의 치타 공부법 후기', now(), now());

-- #38 (공부법) -> seed_studymethod | knou1.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900030, 3, 1, 'https://knou1.tistory.com/2606', '나에게 맞는 공부법은? 다양한 공부법 10가지 추천!', 0, 2, 2, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900030, 900030, '자신에게 맞는 공부법을 찾을 수 있도록 밑줄 긋기 등 10가지 학습법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900030, 2, 2, true, true, true, 0, '나에게 맞는 공부법은? 다양한 공부법 10가지 추천!', now(), now());

-- #39 (공부법) -> seed_studymethod | if-blog.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900031, 3, 1, 'https://if-blog.tistory.com/12424', '뇌과학으로 보는 효율적인 공부법', 0, 2, 4, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900031, 900031, '뇌과학 관점에서 효율적인 공부법이 무엇인지 설명하는 교육부 공식 블로그 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (102, 900031, 2, 4, true, true, true, 0, '뇌과학으로 보는 효율적인 공부법', now(), now());

-- #40 (IT·개발) -> seed_it_dev | coding-self-study.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900032, 4, 1, 'https://coding-self-study.tistory.com/m/5', 'API 응답 통일을 파헤치다', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900032, 900032, '프로젝트에서 API 응답 형식을 통일해야 하는 이유와 구체적인 설계 방법을 정리한 개발 초심자 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900032, 2, 19, true, true, true, 0, 'API 응답 통일을 파헤치다', now(), now());

-- #41 (IT·개발) -> seed_it_dev | www.wired.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900033, 4, 1, 'https://www.wired.com/story/portable-pos-thefts-how-to-protect-yourself-from-scams/', 'How to Protect Yourself From Portable Point-of-Sale Scams', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900033, 900033, '휴대용 POS 단말기를 노린 결제 사기 수법과 이를 예방하는 방법을 설명하는 보안 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900033, 2, 18, true, true, true, 0, 'How to Protect Yourself From Portable Point-of-Sale Scams', now(), now());

-- #42 (IT·개발) -> seed_it_dev | velog.io
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900034, 4, 1, 'https://velog.io/@rewq5991/nodejs-contribution', '4시간 만에 Node.js PR 승인받기', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900034, 900034, 'AI 페어 프로그래밍을 활용해 4시간 만에 Node.js 오픈소스 PR 승인을 받아낸 기여 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900034, 2, 19, true, true, true, 0, '4시간 만에 Node.js PR 승인받기', now(), now());

-- #43 (IT·개발) -> seed_it_dev | cogo.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900035, 4, 1, 'https://cogo.tistory.com/m/6', '서버 인증 방식', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900035, 900035, 'HTTP의 무상태성부터 시작해 서버 인증 방식의 종류와 장단점을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900035, 2, 18, true, true, true, 0, '서버 인증 방식', now(), now());

-- #44 (IT·개발) -> seed_it_dev | emotionte.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900036, 4, 1, 'https://emotionte.com/피그마-스크린샷-플러그인-무료-변환-ai/', '피그마 스크린샷 플러그인 무료 변환 AI', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900036, 900036, '스크린샷을 피그마 파일로 자동 변환해주는 무료 AI 플러그인의 기능과 사용법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900036, 2, 19, true, true, true, 0, '피그마 스크린샷 플러그인 무료 변환 AI', now(), now());

-- #45 (IT·개발) -> seed_it_dev | gamedevlog.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900037, 4, 1, 'https://gamedevlog.tistory.com/44', '모듈로 연산(modulo operation)', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900037, 900037, '모듈로 연산의 개념과 덧셈·뺄셈·곱셈에서의 성질을 정리한 정수론 학습 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900037, 2, 18, true, true, true, 0, '모듈로 연산(modulo operation)', now(), now());

-- #46 (IT·개발) -> seed_it_dev | velog.io
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900038, 4, 1, 'https://velog.io/@2hyunjinn/CodeRabbit-으로-AI-코드리뷰-달기-8lbgc6tl', 'CodeRabbit 으로 AI 코드리뷰 달기 🐰💬', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900038, 900038, '동료 없이 혼자 개발하던 중 CodeRabbit을 도입해 AI 코드 리뷰를 받기 시작한 경험담.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900038, 2, 19, true, true, true, 0, 'CodeRabbit 으로 AI 코드리뷰 달기 🐰💬', now(), now());

-- #47 (IT·개발) -> seed_it_dev | tech.inflab.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900039, 4, 1, 'https://tech.inflab.com/20250303-introduce-coderabbit/', '코드 리뷰 요정, CodeRabbit이 나타났다 🐰', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900039, 900039, 'CodeRabbit이 PR마다 자동으로 스타일, 버그 가능성, 성능 개선점을 리뷰해주는 기능을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900039, 2, 18, true, true, true, 0, '코드 리뷰 요정, CodeRabbit이 나타났다 🐰', now(), now());

-- #48 (IT·개발) -> seed_it_dev | www.itworld.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900040, 4, 1, 'https://www.itworld.co.kr/article/4039484/llm-시리부터-스마트-로봇까지-애플의-ai-반격.html', '‘LLM 시리’부터 스마트 로봇까지… 애플의 AI 반격', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900040, 900040, '시리 개선과 스마트홈, 로봇 사업으로 AI 반격을 노리는 애플의 향후 계획을 다룬 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900040, 2, 19, true, true, true, 0, '‘LLM 시리’부터 스마트 로봇까지… 애플의 AI 반격', now(), now());

-- #49 (IT·개발) -> seed_it_dev | zdnet.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900041, 4, 1, 'https://zdnet.co.kr/view/?no=20250814153523', 'GPU 기반 추론 워크스테이션 ''배틀매트릭스'' 힘 주는 인텔', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900041, 900041, '인텔이 GPU 기반 대규모 언어모델 추론 워크스테이션 ''배틀매트릭스''에 힘을 싣고 있다는 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900041, 2, 18, true, true, true, 0, 'GPU 기반 추론 워크스테이션 ''배틀매트릭스'' 힘 주는 인텔', now(), now());

-- #50 (IT·개발) -> seed_it_dev | gurumee92.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900042, 4, 1, 'https://gurumee92.tistory.com/220', 'Prometheus란 무엇인가', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900042, 900042, '오픈소스 모니터링 시스템 Prometheus가 무엇인지, 아키텍처와 적합한 상황을 정리한 문서.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900042, 2, 19, true, true, true, 0, 'Prometheus란 무엇인가', now(), now());

-- #51 (IT·개발) -> seed_it_dev | gooners0304.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900043, 4, 1, 'https://gooners0304.tistory.com/entry/Prometheus-소개', 'Prometheus 소개', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900043, 900043, 'SoundCloud에서 개발된 시계열 모니터링 도구 Prometheus의 구성 요소와 특징을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900043, 2, 18, true, true, true, 0, 'Prometheus 소개', now(), now());

-- #52 (IT·개발) -> seed_it_dev | velog.io
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900044, 4, 1, 'https://velog.io/@engus525/Redis-Spring에-적용하기', 'Redis Spring에 적용하기', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900044, 900044, 'Redis를 캐싱 용도로 Spring Boot 프로젝트에 설치하고 적용하는 실습 과정을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900044, 2, 19, true, true, true, 0, 'Redis Spring에 적용하기', now(), now());

-- #53 (IT·개발) -> seed_it_dev | curt-poem.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900045, 4, 1, 'https://curt-poem.tistory.com/entry/개인적으로-느낀-Kotlin의-좋은-점과-애매한-점과-별로인-점부제-내가-느낀-최신-프로그래밍-언어의-경향들', '개인적으로 느낀 Kotlin의 좋은 점과 애매한 점과 별로인 점(부제: 내가 느낀', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900045, 900045, '안드로이드 개발자로 커리어를 시작하며 느낀 Kotlin 언어의 장점과 애매한 점을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900045, 2, 18, true, true, true, 0, '개인적으로 느낀 Kotlin의 좋은 점과 애매한 점과 별로인 점(부제: 내가 느낀', now(), now());

-- #54 (IT·개발) -> seed_it_dev | beaholic.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900046, 4, 1, 'https://beaholic.tistory.com/2', 'R 프로그래밍 기초_ R 기본 개념 & 설치', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900046, 900046, '프로그래밍 경험이 없는 사람도 통계·그래프 작업에 활용할 수 있는 R의 기본 개념과 설치법 소개.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (103, 900046, 2, 19, true, true, true, 0, 'R 프로그래밍 기초_ R 기본 개념 & 설치', now(), now());

-- #55 (자기계발) -> seed_selfdev | blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900047, 5, 1, 'https://blog.naver.com/saranmul/222702436544', '조합 놀이:아이디어를 얻는 기법-한 광고인의 경험-오래된 요소들의 결합-배양 단계', 0, 1, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900047, 900047, '오래된 아이디어들을 새롭게 결합해 창의적인 아이디어를 얻는 광고인의 기법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900047, 1, 23, true, true, true, 0, '조합 놀이:아이디어를 얻는 기법-한 광고인의 경험-오래된 요소들의 결합-배양 단계', now(), now());

-- #56 (자기계발) -> seed_selfdev | kes0001.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900048, 5, 1, 'https://kes0001.tistory.com/15', '30대에 꼭 해야 할 자기계발 리스트 10가지', 0, 1, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900048, 900048, '사회생활이 안정되기 시작하는 30대에 꼭 해야 할 자기계발 리스트 10가지를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900048, 1, 20, true, true, true, 0, '30대에 꼭 해야 할 자기계발 리스트 10가지', now(), now());

-- #57 (자기계발) -> seed_selfdev | product.kyobobook.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900049, 5, 1, 'https://product.kyobobook.co.kr/category/KOR/15#?type=home', '자기계발 국내도서 - 교보문고', 0, 1, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900049, 900049, '교보문고가 소개하는 이번 주 화제의 자기계발 국내 신간 도서 모음.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900049, 1, 23, true, true, true, 0, '자기계발 국내도서 - 교보문고', now(), now());

-- #58 (자기계발) -> seed_selfdev | www.success.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900050, 5, 1, 'https://www.success.com/maria-menounos-resilience-healing-growth/', 'Maria Menounos: Building Resilience For a Life That Thrives', 0, 1, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900050, 900050, '여러 커리어를 거치며 회복탄력성을 키워온 마리아 메노노스의 삶을 다룬 인터뷰 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900050, 1, 20, true, true, true, 0, 'Maria Menounos: Building Resilience For a Life That Thrives', now(), now());

-- #59 (자기계발) -> seed_selfdev | effimprove.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900051, 5, 1, 'https://effimprove.tistory.com/2', '효율성 자기계발 시작 이유&논리', 0, 1, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900051, 900051, '시간이 부족한 사람이 짧은 시간에도 고효율을 낼 수 있는 자기계발 방법을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900051, 1, 23, true, true, true, 0, '효율성 자기계발 시작 이유&논리', now(), now());

-- #60 (자기계발) -> seed_selfdev | www.ubob.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900052, 5, 1, 'https://www.ubob.com/insight/detail_view/1211', '[기사] “올해도 갓생이다”…직장인 50.4%, “업무시간에도 자기계발”', 0, 1, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900052, 900052, '직장인 절반 이상이 업무 시간에도 자기계발을 한다는 ''갓생'' 트렌드를 다룬 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900052, 1, 20, true, true, true, 0, '[기사] “올해도 갓생이다”…직장인 50.4%, “업무시간에도 자기계발”', now(), now());

-- #61 (자기계발) -> seed_selfdev | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900053, 5, 1, 'https://m.blog.naver.com/bookteadream/222452890952', '자기계발/버킷리스트(2022/10/31 갱신)', 0, 1, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900053, 900053, '블로그를 시작하며 정리한 개인적인 자기계발 및 버킷리스트 기록.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900053, 1, 23, true, true, true, 0, '자기계발/버킷리스트(2022/10/31 갱신)', now(), now());

-- #62 (자기계발) -> seed_selfdev | community.linkareer.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900054, 5, 1, 'https://community.linkareer.com/mentor_employed/2528635', '신입한테 추천하는 자기계발 리스트', 0, 1, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900054, 900054, '취업 준비생을 위해 신입사원에게 추천하는 자기계발 리스트를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900054, 1, 20, true, true, true, 0, '신입한테 추천하는 자기계발 리스트', now(), now());

-- #65 (자기계발) -> seed_selfdev | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900055, 5, 1, 'https://brunch.co.kr/@dong02/1612', '책 읽는 방법 - 5가지 독서법', 0, 1, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900055, 900055, '시간이 없거나 귀찮아서 책을 못 읽는 사람들을 위한 다섯 가지 독서법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900055, 1, 23, true, true, true, 0, '책 읽는 방법 - 5가지 독서법', now(), now());

-- #66 (자기계발) -> seed_selfdev | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900056, 5, 1, 'https://brunch.co.kr/@5f4fe4acc0ae43c/3', '대학생,직장인을 위한 뉴스 플랫폼 추천', 0, 1, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900056, 900056, '국제 정세를 다루는 기자가 대학생과 직장인에게 추천하는 해외 뉴스 플랫폼들을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (104, 900056, 1, 20, true, true, true, 0, '대학생,직장인을 위한 뉴스 플랫폼 추천', now(), now());

-- #67 (취업·이직) -> seed_career | www.superookie.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900057, 6, 1, 'https://www.superookie.com/contents/60f7b3598b129f0dbc0db334', '이직을 생각하는 1,2년차를 위해', 0, 2, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900057, 900057, '이직을 고민하는 1, 2년차 직장인이 먼저 점검해야 할 이직 동기와 판단 기준을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900057, 2, 41, true, true, true, 0, '이직을 생각하는 1,2년차를 위해', now(), now());

-- #68 (취업·이직) -> seed_career | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900058, 6, 1, 'https://brunch.co.kr/@basic2sic/69', '이직을 하면 아까울 유형 5가지', 0, 5, 42, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900058, 900058, '무턱대고 이직했다가 후회하기 쉬운 다섯 가지 유형을 소개하며 신중한 이직을 조언하는 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900058, 5, 42, true, true, true, 0, '이직을 하면 아까울 유형 5가지', now(), now());

-- #69 (취업·이직) -> seed_career | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900059, 6, 1, 'https://m.blog.naver.com/xoxo_pch/222681850178', '깔끔하게 퇴사 준비하고 퇴사 절차 밟는 법', 0, 2, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900059, 900059, '처음 이직하는 사람을 위해 티 나지 않게 퇴사를 준비하고 절차를 밟는 방법을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900059, 2, 41, true, true, true, 0, '깔끔하게 퇴사 준비하고 퇴사 절차 밟는 법', now(), now());

-- #70 (취업·이직) -> seed_career | www.jobkorea.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900060, 6, 1, 'https://www.jobkorea.co.kr/Recruit/GI_Read/47458134?sc=224', '주식회사 핀즐 UX/UI/웹 디자이너 경력', 0, 2, 42, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900060, 900060, 'UX/UI 웹 디자이너 경력직을 채용하며 합격 축하금을 내건 회사의 채용 공고.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900060, 2, 42, true, true, true, 0, '주식회사 핀즐 UX/UI/웹 디자이너 경력', now(), now());

-- #71 (취업·이직) -> seed_career | www.nhis.or.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900061, 6, 1, 'https://www.nhis.or.kr/nhis/together/wbhaea02700m01.do?mode=view&articleNo=11004457', '2025년도 하반기 국민건강보험공단 신규직원 채용 공고', 0, 2, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900061, 900061, '국민건강보험공단이 발표한 2025년 하반기 신규 직원 채용 공고와 접수 안내.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900061, 2, 41, true, true, true, 0, '2025년도 하반기 국민건강보험공단 신규직원 채용 공고', now(), now());

-- #72 (취업·이직) -> seed_career | blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900062, 6, 1, 'https://blog.naver.com/hans_way/223142841618', '실업급여 실업인정 활동종류(구직활동 및 구직활동 외 활동)와 하는 방법(+팁)', 0, 2, 42, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900062, 900062, '실업급여를 받으며 실업인정으로 인정되는 구직·구직 외 활동의 종류와 진행 팁을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900062, 2, 42, true, true, true, 0, '실업급여 실업인정 활동종류(구직활동 및 구직활동 외 활동)와 하는 방법(+팁)', now(), now());

-- #73 (취업·이직) -> seed_career | www.jobkorea.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900063, 6, 1, 'https://www.jobkorea.co.kr/starter/passassay?schTxt=&Page=1', '합격자소서', 0, 2, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900063, 900063, '금융결제원 등 기업별 지원 동기 문항과 합격 자소서 사례를 모아 놓은 자료.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900063, 2, 41, true, true, true, 0, '합격자소서', now(), now());

-- #74 (취업·이직) -> seed_career | blog.opensurvey.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900064, 6, 1, 'https://blog.opensurvey.co.kr/trendreport/job-search-2022/', '취업·이직 트렌드 리포트 2022', 0, 2, 42, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900064, 900064, '채용 전쟁 시대에 직장인들이 이직을 고민하는 이유와 채용 트렌드를 분석한 리포트 소개.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900064, 2, 42, true, true, true, 0, '취업·이직 트렌드 리포트 2022', now(), now());

-- #75 (취업·이직) -> seed_career | www.wanted.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900065, 6, 1, 'https://www.wanted.co.kr/wd/301979', '커머스사 담당 퍼포먼스 마케팅AE (과장급)', 0, 2, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900065, 900065, '디지털 마케팅 기업이 커머스사 담당 과장급 퍼포먼스 마케팅 경력직을 채용하는 공고.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900065, 2, 41, true, true, true, 0, '커머스사 담당 퍼포먼스 마케팅AE (과장급)', now(), now());

-- #76 (취업·이직) -> seed_career | www.jobplanet.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900066, 6, 1, 'https://www.jobplanet.co.kr/contents/news-3419/나 지금%2C 이직할 때일까요%3F', '나 지금, 이직할 때일까요? [이직 타이밍 알아보기] 이직 쿨타임 찼을까? 3분 안에 진단해드림!', 0, 4, 42, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900066, 900066, '지금이 이직할 때인지 헷갈리는 직장인을 위해 이직 타이밍을 진단해보는 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900066, 4, 42, true, true, true, 0, '나 지금, 이직할 때일까요? [이직 타이밍 알아보기] 이직 쿨타임 찼을까? 3분 안에 진단해드림!', now(), now());

-- #77 (취업·이직) -> seed_career | community.rememberapp.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900067, 6, 1, 'https://community.rememberapp.co.kr/post/152929', '나이 50에 이직을 하며 느낀 점', 0, 4, 41, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900067, 900067, '만 49세에 직무 불일치로 퇴사한 뒤 10개월간의 구직 끝에 중소기업으로 이직한 경험담.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (105, 900067, 4, 41, true, true, true, 0, '나이 50에 이직을 하며 느낀 점', now(), now());

-- #78 (비즈니스 인사이트) -> seed_biz_insight | economydaddy.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900068, 7, 1, 'https://economydaddy.tistory.com/m/291', '부의 3요소', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900068, 900068, '자본주의 사회에서 부를 이루기 위해 필요한 세 가지 요소를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900068, 2, 19, true, true, true, 0, '부의 3요소', now(), now());

-- #79 (비즈니스 인사이트) -> seed_biz_insight | velog.io
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900069, 7, 1, 'https://velog.io/@hello1234/뉴스레터로-연-60억-대체-어떻게', '🗞️ 뉴스레터로 연 60억 대체 어떻게?', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900069, 900069, '뉴스레터 하나로 연 60억원의 수익을 만들어낸 Lenny의 구독·광고 비즈니스 모델을 분석한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900069, 2, 20, true, true, true, 0, '🗞️ 뉴스레터로 연 60억 대체 어떻게?', now(), now());

-- #80 (비즈니스 인사이트) -> seed_biz_insight | zapier.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900070, 7, 1, 'https://zapier.com/blog/zapier-ai-orchestration-platform/', 'Meet Zapier''s AI orchestration platform: Add AI agents to nearly 8,000 apps', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900070, 900070, '재피어(Zapier)가 8000개에 달하는 앱에 AI 에이전트를 붙이는 오케스트레이션 플랫폼을 출시했다는 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900070, 2, 19, true, true, true, 0, 'Meet Zapier''s AI orchestration platform: Add AI agents to nearly 8,000 apps', now(), now());

-- #81 (비즈니스 인사이트) -> seed_biz_insight | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900071, 7, 1, 'https://brunch.co.kr/@sparta/65', '혁명적 메모 애플리케이션, 옵시디언 설치와 사용법 정리', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900071, 900071, '마크업 기반 메모 앱 옵시디언의 설치 방법과 기본 사용법을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900071, 2, 20, true, true, true, 0, '혁명적 메모 애플리케이션, 옵시디언 설치와 사용법 정리', now(), now());

-- #82 (비즈니스 인사이트) -> seed_biz_insight | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900072, 7, 1, 'https://brunch.co.kr/@jeongggjae/17', '슬랙 제대로 사용하기(1) - 채널 구성과 룰 세팅', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900072, 900072, '협업 도구 슬랙을 제대로 쓰기 위한 채널 구성과 팀 룰 세팅 방법을 다룬 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900072, 2, 19, true, true, true, 0, '슬랙 제대로 사용하기(1) - 채널 구성과 룰 세팅', now(), now());

-- #84 (비즈니스 인사이트) -> seed_biz_insight | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900073, 7, 1, 'https://m.blog.naver.com/studio_pan/222188522757', '포토샵 도장툴 사용하기', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900073, 900073, '복제나 자연스러운 보정에 쓰이는 포토샵 도장툴의 사용법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900073, 2, 20, true, true, true, 0, '포토샵 도장툴 사용하기', now(), now());

-- #85 (비즈니스 인사이트) -> seed_biz_insight | ccracker.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900074, 7, 1, 'https://ccracker.tistory.com/entry/노션-같은-툴-당신에게-딱-맞는-대안은', '노션 같은 툴, 당신에게 딱 맞는 대안은?', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900074, 900074, '노션의 대안이 될 만한 생산성·협업 툴 몇 가지를 비교해 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900074, 2, 19, true, true, true, 0, '노션 같은 툴, 당신에게 딱 맞는 대안은?', now(), now());

-- #86 (비즈니스 인사이트) -> seed_biz_insight | everybody-yeah.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900075, 7, 1, 'https://everybody-yeah.tistory.com/21', '[앱 분석하기 #1] 유튜브 뮤직 분석하기', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900075, 900075, '포트폴리오 작성을 위해 유튜브 뮤직 앱을 직접 분석해본 개인 프로젝트 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900075, 2, 20, true, true, true, 0, '[앱 분석하기 #1] 유튜브 뮤직 분석하기', now(), now());

-- #87 (비즈니스 인사이트) -> seed_biz_insight | ordinary-code.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900076, 7, 1, 'https://ordinary-code.tistory.com/132', '[Notion] 노션에서 토글 사용하기(토글 제목1,2,3 업데이트 추가!)', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900076, 900076, '노션에서 토글 블록을 만들고 활용하는 방법과 단축키를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900076, 2, 19, true, true, true, 0, '[Notion] 노션에서 토글 사용하기(토글 제목1,2,3 업데이트 추가!)', now(), now());

-- #88 (비즈니스 인사이트) -> seed_biz_insight | solapi.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900077, 7, 1, 'https://solapi.com/blog/japieo-zapier-gibon-sayongbeob/', '재피어(Zapier) 소개와 기본 사용법', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900077, 900077, '여러 앱을 자동으로 연결해주는 자동화 플랫폼 Zapier의 소개와 기본 사용법.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900077, 2, 20, true, true, true, 0, '재피어(Zapier) 소개와 기본 사용법', now(), now());

-- #89 (비즈니스 인사이트) -> seed_biz_insight | www.notion.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900078, 7, 1, 'https://www.notion.com/ko/help/guides', '가이드 Notion 사용법을 익히고 다양하게 활용해 보세요.', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900078, 900078, 'AI 에이전트를 활용해 노션에서 데이터베이스 구축 등 복잡한 작업을 처리하는 방법을 소개한 가이드.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900078, 2, 19, true, true, true, 0, '가이드 Notion 사용법을 익히고 다양하게 활용해 보세요.', now(), now());

-- #90 (비즈니스 인사이트) -> seed_biz_insight | www.figmapedia.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900079, 7, 1, 'https://www.figmapedia.com/5f1fee0c-4a02-4a6d-b603-ac250bd5c0a8', '유용한 기획/QA 문서 템플릿', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900079, 900079, '디자인 외 기획·QA 단계에서 쓸 수 있는 유용한 문서 템플릿들을 모아 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900079, 2, 20, true, true, true, 0, '유용한 기획/QA 문서 템플릿', now(), now());

-- #91 (비즈니스 인사이트) -> seed_biz_insight | blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900080, 7, 1, 'https://blog.naver.com/blogyourlife/222612642908', '업무 자동화 <구글스프레드시트 QA 테스트 양식 (템플릿)>', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900080, 900080, '구글 스프레드시트로 만든 QA 테스트 양식 템플릿과 활용 방법을 공유한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900080, 2, 19, true, true, true, 0, '업무 자동화 <구글스프레드시트 QA 테스트 양식 (템플릿)>', now(), now());

-- #92 (비즈니스 인사이트) -> seed_biz_insight | www.clien.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900081, 7, 1, 'https://www.clien.net/service/board/use/18517111', '옵시디언 간단 사용기', 0, 2, 20, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900081, 900081, '여러 번 자리를 옮기며 기록이 사라지는 경험을 하다 옵시디언을 도입하게 된 프리랜서의 사용기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (106, 900081, 2, 20, true, true, true, 0, '옵시디언 간단 사용기', now(), now());

-- #93 (라이프스타일) -> seed_lifestyle | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900082, 9, 1, 'https://m.blog.naver.com/ckgusxo0420/222167359034', '스마트폰만 있어도 돼; 핸드폰 사진 잘 찍는법 50가지 팁', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900082, 900082, '수평 맞추기 등 스마트폰만으로 사진을 잘 찍을 수 있는 50가지 팁을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900082, 1, 39, true, true, true, 0, '스마트폰만 있어도 돼; 핸드폰 사진 잘 찍는법 50가지 팁', now(), now());

-- #95 (라이프스타일) -> seed_lifestyle | kcccolorndesign.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900083, 9, 1, 'https://kcccolorndesign.com/entry/나의-행복과-삶에-집중하는-On-my-own', '라이프스타일 트렌드 ㅣ 나의 행복과 삶에 집중하는 ''On my own''', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900083, 900083, '물질의 소유보다 경험과 정신적 만족을 추구하는 밀레니얼·Z세대의 라이프스타일 트렌드를 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900083, 1, 34, true, true, true, 0, '라이프스타일 트렌드 ㅣ 나의 행복과 삶에 집중하는 ''On my own''', now(), now());

-- #96 (라이프스타일) -> seed_lifestyle | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900084, 9, 1, 'https://m.blog.naver.com/PostView.naver?blogId=eowner&logNo=223189416461&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '케이카 워런티 보증 기간 만료 전 점검하기 : 케이카 워런티 신청하고 점검받기', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900084, 900084, '중고차 구매 후 워런티 보증 기간이 끝나기 전 점검을 받은 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900084, 1, 39, true, true, true, 0, '케이카 워런티 보증 기간 만료 전 점검하기 : 케이카 워런티 신청하고 점검받기', now(), now());

-- #97 (라이프스타일) -> seed_lifestyle | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900085, 9, 1, 'https://m.blog.naver.com/sheska/221719460431', '인테리어 시작하기. 기본과 기초', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900085, 900085, '인스타그램 참고, 수납 비율 등 인테리어를 시작할 때 알아두면 좋은 기본기를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900085, 1, 34, true, true, true, 0, '인테리어 시작하기. 기본과 기초', now(), now());

-- #99 (라이프스타일) -> seed_lifestyle | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900086, 9, 1, 'https://m.blog.naver.com/rlawlsdk0216/222688309529', '쏘카(SOCAR) 쏘카존 탑승예약 및 이용방법, 비용까지 자세한 이용후기', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900086, 900086, '차량 공유 서비스 쏘카존의 예약·이용 방법과 비용을 자세히 정리한 이용 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900086, 1, 39, true, true, true, 0, '쏘카(SOCAR) 쏘카존 탑승예약 및 이용방법, 비용까지 자세한 이용후기', now(), now());

-- #100 (라이프스타일) -> seed_lifestyle | m.oliveyoung.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900087, 9, 1, 'https://m.oliveyoung.co.kr/m/mtn/magazine/editorial/100123766', '2024 올영 상반기 결산! 가장 많이 팔린 스킨케어 TOP3는?', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900087, 900087, '2024년 상반기 올리브영에서 가장 많이 팔린 스킨케어 제품 top3를 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900087, 1, 34, true, true, true, 0, '2024 올영 상반기 결산! 가장 많이 팔린 스킨케어 TOP3는?', now(), now());

-- #101 (라이프스타일) -> seed_lifestyle | economychosun.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900088, 9, 1, 'https://economychosun.com/site/data/html_dir/2025/02/07/2025020700033.html', '라이프스타일 브랜드의 엉뚱한 브랜딩, 무인양품', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900088, 900088, '저성장 시대에 소비자의 신념과 가치관을 반영하는 라이프스타일 브랜드 무인양품의 사례를 다룬 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900088, 1, 39, true, true, true, 0, '라이프스타일 브랜드의 엉뚱한 브랜딩, 무인양품', now(), now());

-- #102 (라이프스타일) -> seed_lifestyle | nzine.kpipa.or.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900089, 9, 1, 'https://nzine.kpipa.or.kr/sub/coverstory.php?ptype=view&idx=934&code=coverstory&category=', '[시니어 독서 시장의 현재와 미래] 노인도 읽고 싶습니다', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900089, 900089, '초고령 사회에 접어든 한국에서 시니어를 위한 독서 시장의 현재와 미래를 짚은 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900089, 1, 34, true, true, true, 0, '[시니어 독서 시장의 현재와 미래] 노인도 읽고 싶습니다', now(), now());

-- #103 (라이프스타일) -> seed_lifestyle | www.civicnews.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900090, 9, 1, 'http://www.civicnews.com/news/articleView.html?idxno=35830', '2030세대의 새로운 트렌드 ‘식집사’... 이제는 반려 식물 시대', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900090, 900090, '반려 식물을 가족처럼 돌보는 ''식집사'' 문화가 2030세대 사이에서 트렌드로 자리잡고 있다는 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900090, 1, 39, true, true, true, 0, '2030세대의 새로운 트렌드 ‘식집사’... 이제는 반려 식물 시대', now(), now());

-- #104 (라이프스타일) -> seed_lifestyle | www.hani.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900091, 9, 1, 'https://www.hani.co.kr/arti/well/news/1127907.html', '삶을 바꾸는 5가지 명상법', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900091, 900091, '명상을 통해 우울과 분노에서 벗어난 명상가의 경험을 담은 책 ''삶을 바꾸는 5가지 명상법'' 소개.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900091, 1, 34, true, true, true, 0, '삶을 바꾸는 5가지 명상법', now(), now());

-- #105 (라이프스타일) -> seed_lifestyle | m.health.chosun.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900092, 9, 1, 'https://m.health.chosun.com/svc/news_view.html?contid=2025081302162', '“50대에도 탄탄한 ‘등 근육’”… 요가만 10년 했다는 유명 여배우, 누구?', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900092, 900092, '10년 가까이 요가를 해온 배우 김지호가 근황과 산문집 출간 소식을 전한 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900092, 1, 39, true, true, true, 0, '“50대에도 탄탄한 ‘등 근육’”… 요가만 10년 했다는 유명 여배우, 누구?', now(), now());

-- #106 (라이프스타일) -> seed_lifestyle | inonstop.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900093, 9, 1, 'https://inonstop.tistory.com/entry/다이소-여행용-지퍼백으로-실속-챙기기', '다이소 여행용 지퍼백으로 실속 챙기기', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900093, 900093, '다이소에서 파는 귀여운 캐릭터 여행용 지퍼백들을 소개하는 개인 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900093, 1, 34, true, true, true, 0, '다이소 여행용 지퍼백으로 실속 챙기기', now(), now());

-- #107 (라이프스타일) -> seed_lifestyle | product.kyobobook.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900094, 9, 1, 'https://product.kyobobook.co.kr/detail/S000217184058', '주문하신 복근 나왔습니다', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900094, 900094, '운동 초보자의 눈높이에 맞춘 건강 만화책 ''주문하신 복근 나왔습니다'' 출간 소식.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900094, 1, 39, true, true, true, 0, '주문하신 복근 나왔습니다', now(), now());

-- #108 (라이프스타일) -> seed_lifestyle | event.kyobobook.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900095, 9, 1, 'https://event.kyobobook.co.kr/funding/detail/295', '이나피스퀘어, 익숙한 일상을 다시 바라보는 법 이나피스퀘어와 비온뒤, 백상점 그리고 교보문고가 함께한 단독 굿즈 최초공개', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900095, 900095, '이나피스퀘어와 여러 브랜드가 협업한 교보문고 단독 굿즈 펀딩과 배송 일정 안내.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900095, 1, 34, true, true, true, 0, '이나피스퀘어, 익숙한 일상을 다시 바라보는 법 이나피스퀘어와 비온뒤, 백상점 그리고 교보문고가 함께한 단독 굿즈 최초공개', now(), now());

-- #109 (라이프스타일) -> seed_lifestyle | www.nhis.or.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900096, 9, 1, 'https://www.nhis.or.kr/nhis/together/wbhaec01000m01.do', '정보공개제도안내', 0, 1, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900096, 900096, '국민의 알권리 보장을 위해 공공기관 정보를 열람·제공하는 정보공개제도를 안내하는 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900096, 1, 39, true, true, true, 0, '정보공개제도안내', now(), now());

-- #110 (라이프스타일) -> seed_lifestyle | www.clien.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900097, 9, 1, 'https://www.clien.net/service/board/cm_car/17607879', '쏘카 2달 사용 후기', 0, 1, 34, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900097, 900097, '차량 출고가 늦어지는 동안 두 달간 쏘카를 이용해본 뒤 느낀 불편한 점을 정리한 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (107, 900097, 1, 34, true, true, true, 0, '쏘카 2달 사용 후기', now(), now());

-- #111 (심리·자기이해) -> seed_psych | sunsooklee.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900098, 10, 1, 'https://sunsooklee.tistory.com/65', '사람 풍경 -심리여행 에세이/ 김형경 - 예담 刊', 0, 2, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900098, 900098, '작가 김형경의 심리 에세이 ''사람 풍경''을 우연히 집어 들고 읽게 된 계기를 담은 독후감.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900098, 2, 47, true, true, true, 0, '사람 풍경 -심리여행 에세이/ 김형경 - 예담 刊', now(), now());

-- #112 (심리·자기이해) -> seed_psych | orwell.distancing.im
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900099, 10, 1, 'https://orwell.distancing.im/blog/types-of-emotions-in-psychology', '심리학에서 이야기하는 감정의 종류 - 163가지 감정 단어 모음', 0, 2, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900099, 900099, '인지심리학에서 분류하는 긍정·부정 감정 163가지 단어를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900099, 2, 45, true, true, true, 0, '심리학에서 이야기하는 감정의 종류 - 163가지 감정 단어 모음', now(), now());

-- #113 (심리·자기이해) -> seed_psych | www.donga.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900100, 10, 1, 'https://www.donga.com/news/It/article/all/20230115/117438122/1', '사소한 일로 폭발하듯 ''버럭''하는 당신, 분노조절장애?[최고야의 심심 토크)', 0, 5, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900100, 900100, '사소한 일에도 크게 화를 내는 사람이 짚어봐야 할 분노조절장애 가능성을 다룬 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900100, 5, 47, true, true, true, 0, '사소한 일로 폭발하듯 ''버럭''하는 당신, 분노조절장애?[최고야의 심심 토크)', now(), now());

-- #114 (심리·자기이해) -> seed_psych | www.amc.seoul.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900101, 10, 1, 'https://www.amc.seoul.kr/asan/depts/mind/K/bbsDetail.do?menuId=4548&contentId=254263', '[정신건강칼럼 7월] 감정 다스리기', 0, 2, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900101, 900101, '감정에 휩쓸리지 않고 평정을 유지하고 싶어하는 사람들을 위한 감정 다스리기 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900101, 2, 45, true, true, true, 0, '[정신건강칼럼 7월] 감정 다스리기', now(), now());

-- #115 (심리·자기이해) -> seed_psych | www.mindgil.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900102, 10, 1, 'https://www.mindgil.com/news/articleView.html?idxno=90206', '“위대한 성취는 안전지대 벗어날 때 가능하다”', 0, 2, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900102, 900102, '넷플릭스 ''웬즈데이''로 큰 성공을 거둔 배우 제나 오르테가의 이야기를 통해 본 성취의 조건.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900102, 2, 47, true, true, true, 0, '“위대한 성취는 안전지대 벗어날 때 가능하다”', now(), now());

-- #116 (심리·자기이해) -> seed_psych | www.nhis.or.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900103, 10, 1, 'https://www.nhis.or.kr/magazin/162/html/sub3.html#:~:text=가만히 있지 못하는 과잉행동 및 생각 없이,나이에 따라 예상되는 정도보다 현저하게 심하게 나타납니다.', '전문가 톡톡 성인에게도 많은 ADHD', 0, 2, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900103, 900103, '소아기뿐 아니라 성인에게도 나타날 수 있는 ADHD의 특징과 치료를 다룬 전문가 인터뷰.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900103, 2, 45, true, true, true, 0, '전문가 톡톡 성인에게도 많은 ADHD', now(), now());

-- #117 (심리·자기이해) -> seed_psych | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900104, 10, 1, 'https://m.blog.naver.com/suinjae00/221601070922', '주의력결핍 과잉행동장애, 종류가 다르다고요?', 0, 2, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900104, 900104, '주의력결핍 과잉행동장애(ADHD)에도 여러 종류가 있다는 것을 설명하는 전문 클리닉 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900104, 2, 47, true, true, true, 0, '주의력결핍 과잉행동장애, 종류가 다르다고요?', now(), now());

-- #119 (심리·자기이해) -> seed_psych | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900105, 10, 1, 'https://brunch.co.kr/@worknlife/165', '자기이해를 위한 기록의 힘', 0, 2, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900105, 900105, '자기 자신을 잘 아는 것이 커리어의 핵심이라며 기록을 통한 자기이해 방법을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900105, 2, 45, true, true, true, 0, '자기이해를 위한 기록의 힘', now(), now());

-- #120 (심리·자기이해) -> seed_psych | ko.wikipedia.org
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900106, 10, 1, 'https://ko.wikipedia.org/wiki/간헐적_폭발성_장애', '간헐적 폭발성 장애 - 위키백과, 우리 모두의 백과사전', 0, 2, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900106, 900106, '간헐적 폭발성 장애의 정의와 특징을 설명하는 백과사전 문서.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900106, 2, 47, true, true, true, 0, '간헐적 폭발성 장애 - 위키백과, 우리 모두의 백과사전', now(), now());

-- #121 (심리·자기이해) -> seed_psych | www.psychiatricnews.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900107, 10, 1, 'http://www.psychiatricnews.net/news/articleView.html?idxno=12379', '마음이 불안정할 때, 절대 하지 말아야 할 5가지', 0, 4, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900107, 900107, '정신건강의학과 전문의가 마음이 불안정할 때 절대 하지 말아야 할 다섯 가지를 정리한 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900107, 4, 45, true, true, true, 0, '마음이 불안정할 때, 절대 하지 말아야 할 5가지', now(), now());

-- #122 (심리·자기이해) -> seed_psych | www.a-ha.io
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900108, 10, 1, 'https://www.a-ha.io/questions/4142ebb747d369fe8e4ca77d93add8d7', '심리상태가 불안정한데 어떻게 해야하나요? ㅣ 궁금할 땐, 아하!', 0, 4, 47, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900108, 900108, '심리상태가 불안정할 때 어떻게 해야 하는지 묻는 질문에 상담사가 답변한 Q&A 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900108, 4, 47, true, true, true, 0, '심리상태가 불안정한데 어떻게 해야하나요? ㅣ 궁금할 땐, 아하!', now(), now());

-- #123 (심리·자기이해) -> seed_psych | www.김형근예병원.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900109, 10, 1, 'http://www.김형근예병원.com/Module/News/Lecture.asp?Mode=V&Srno=5697', '김형근 예병원', 0, 2, 45, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900109, 900109, '높은 곳에서 극도의 불안과 공포를 느끼는 고소공포증의 증상과 원인을 설명한 병원 콘텐츠.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (108, 900109, 2, 45, true, true, true, 0, '김형근 예병원', now(), now());

-- #124 (에세이·칼럼) -> seed_essay_column | www.hani.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900110, 11, 1, 'https://www.hani.co.kr/arti/opinion/column/1210361.html', '여름, 계절의 제왕 [포토에세이]', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900110, 900110, '초록이 짙어지고 꽃이 무성해지는 여름의 생명력을 그린 포토에세이.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900110, 2, 39, true, true, true, 0, '여름, 계절의 제왕 [포토에세이]', now(), now());

-- #125 (에세이·칼럼) -> seed_essay_column | www.hani.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900111, 11, 1, 'https://www.hani.co.kr/arti/opinion/column/1211029.html', '‘케데헌’은 혐오·배제 아닌 조화·포용에서 자랐다 [안병욱 칼럼]', 0, 2, 36, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900111, 900111, '한국 사회의 국제적 활약이 혐오와 배제가 아닌 조화와 포용에서 비롯됐다고 짚은 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900111, 2, 36, true, true, true, 0, '‘케데헌’은 혐오·배제 아닌 조화·포용에서 자랐다 [안병욱 칼럼]', now(), now());

-- #126 (에세이·칼럼) -> seed_essay_column | v.daum.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900112, 11, 1, 'https://v.daum.net/v/20250731192219735', '[금요 에세이] 나를 비추다- 서영덕(수필가)', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900112, 900112, '오랫동안 자신의 얼굴을 마주하지 못했던 수필가가 스스로를 비추어보게 된 계기를 담은 에세이.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900112, 2, 39, true, true, true, 0, '[금요 에세이] 나를 비추다- 서영덕(수필가)', now(), now());

-- #127 (에세이·칼럼) -> seed_essay_column | v.daum.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900113, 11, 1, 'https://v.daum.net/v/20250731165406158', '[내향인으로 살아남기] 내향인들로만 구성된 회사 독서모임, 말은 하냐고요?', 0, 2, 36, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900113, 900113, '내향인들로만 구성된 회사 독서모임이 실제로 어떻게 대화를 나누는지 소개한 에세이.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900113, 2, 36, true, true, true, 0, '[내향인으로 살아남기] 내향인들로만 구성된 회사 독서모임, 말은 하냐고요?', now(), now());

-- #128 (에세이·칼럼) -> seed_essay_column | www.mindgil.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900114, 11, 1, 'https://www.mindgil.com/news/articleView.html?idxno=90173', '[엄상익의 마음길따라 세월따라] 노년의 백수가 자유인 되는 법', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900114, 900114, '노년의 ''백수'' 생활이 진짜 자유인의 삶이 되기 위한 조건을 짚은 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900114, 2, 39, true, true, true, 0, '[엄상익의 마음길따라 세월따라] 노년의 백수가 자유인 되는 법', now(), now());

-- #130 (에세이·칼럼) -> seed_essay_column | www.hkrecruit.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900115, 11, 1, 'http://www.hkrecruit.co.kr/news/articleView.html?idxno=12199', '20대의 자기이해 에세이(10)', 0, 2, 36, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900115, 900115, '타인과의 관계 속에서 자기 자신을 알아가는 자기탐색 방법을 다룬 에세이 연작.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900115, 2, 36, true, true, true, 0, '20대의 자기이해 에세이(10)', now(), now());

-- #131 (에세이·칼럼) -> seed_essay_column | m.cafe.daum.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900116, 11, 1, 'https://m.cafe.daum.net/9595kimmini0A0/N7ia/1722', '＜자영업인생＞ 칼럼니스트가 되기 위해선 어떻게 해야하나', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900116, 900116, '칼럼니스트가 되고 싶다는 문의에 답하기 위해 정리한 칼럼니스트 되는 법.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900116, 2, 39, true, true, true, 0, '＜자영업인생＞ 칼럼니스트가 되기 위해선 어떻게 해야하나', now(), now());

-- #133 (에세이·칼럼) -> seed_essay_column | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900117, 11, 1, 'https://m.blog.naver.com/graymarket/222946320223', '에세이 베스트셀러 | 2022 에세이 추천', 0, 2, 36, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900117, 900117, '온라인 서점 판매량을 기준으로 한 해를 결산한 2022년 에세이 베스트셀러 추천 목록.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900117, 2, 36, true, true, true, 0, '에세이 베스트셀러 | 2022 에세이 추천', now(), now());

-- #136 (에세이·칼럼) -> seed_essay_column | v.daum.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900118, 11, 1, 'https://v.daum.net/v/20250804110703325', '[IT과학칼럼] 우주식품, 미래 식량과 우주 생태계의 연결고리', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900118, 900118, '아르테미스 프로젝트 등 우주 개척 시대에 우주식품이 미래 식량과 어떤 연결고리를 갖는지 다룬 과학칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900118, 2, 39, true, true, true, 0, '[IT과학칼럼] 우주식품, 미래 식량과 우주 생태계의 연결고리', now(), now());

-- #137 (에세이·칼럼) -> seed_essay_column | v.daum.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900119, 11, 1, 'https://v.daum.net/v/20250818110237051', '[IT과학칼럼] 성장과 안전의 마지노선 ‘1.5%p, 1.5℃’를 위한 NBI', 0, 2, 36, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900119, 900119, '잠재성장률 하락과 기후 목표라는 두 마지노선을 함께 다룬 성장과 안전에 관한 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900119, 2, 36, true, true, true, 0, '[IT과학칼럼] 성장과 안전의 마지노선 ‘1.5%p, 1.5℃’를 위한 NBI', now(), now());

-- #138 (에세이·칼럼) -> seed_essay_column | www.hdec.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900120, 11, 1, 'https://www.hdec.kr/KR/newsroom/news_view.aspx?NewsSeq=281&NewsType=TREND&NewsListType=news_list', '[과학칼럼] 과학이 바꾸는 인류의 삶 ‘역사성·우연성·불확실성’ >  > 현대건설 뉴스룸', 0, 2, 39, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900120, 900120, 'X선, 하버법 질소비료, 페니실린 등 인류 역사를 바꾼 과학적 발견들을 정리한 과학 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (109, 900120, 2, 39, true, true, true, 0, '[과학칼럼] 과학이 바꾸는 인류의 삶 ‘역사성·우연성·불확실성’ >  > 현대건설 뉴스룸', now(), now());

-- #139 (트렌드) -> seed_trend | blog.tason.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900121, 12, 1, 'https://blog.tason.com/wordpress/2025-트렌드-코리아/', '한눈에 보는 2025 트렌드 키워드 (트렌드코리아2025) - 휴머스온 블로그ㅣTasOn부터 TMS까지', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900121, 900121, '2025년을 이끌 주요 소비 트렌드 10가지를 요약해 소개한 마케팅 블로그 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900121, 3, 18, true, true, true, 0, '한눈에 보는 2025 트렌드 키워드 (트렌드코리아2025) - 휴머스온 블로그ㅣTasOn부터 TMS까지', now(), now());

-- #140 (트렌드) -> seed_trend | www.careet.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900122, 12, 1, 'https://www.careet.net/1704', '요즘 유행하는 밈이 뭐더라?  검색으로는 안 나오는 최신 밈 모아옴', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900122, 900122, '검색으로는 잘 나오지 않는 최근 SNS 유행 밈들을 모아 뜻과 유래를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900122, 3, 21, true, true, true, 0, '요즘 유행하는 밈이 뭐더라?  검색으로는 안 나오는 최신 밈 모아옴', now(), now());

-- #141 (트렌드) -> seed_trend | www.careet.net
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900123, 12, 1, 'https://www.careet.net/1678', '올해의 메가 트렌드는?  한 장으로 정리하는 하반기 핵심 키워드', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900123, 900123, '빠르게 사라지는 마이크로 트렌드 속에서 오래 지속될 하반기 메가 트렌드를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900123, 3, 18, true, true, true, 0, '올해의 메가 트렌드는?  한 장으로 정리하는 하반기 핵심 키워드', now(), now());

-- #142 (트렌드) -> seed_trend | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900124, 12, 1, 'https://brunch.co.kr/@jordan777/58', '구글 트렌드와 빅데이터', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900124, 900124, '구글 트렌드라는 무료 빅데이터 분석 서비스가 무엇이고 왜 만들어졌는지 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900124, 3, 21, true, true, true, 0, '구글 트렌드와 빅데이터', now(), now());

-- #143 (트렌드) -> seed_trend | trends.withgoogle.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900125, 12, 1, 'https://trends.withgoogle.com/ko/year-in-search/2024/kr/', 'Google 올해의 검색어', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900125, 900125, 'K팝, 레시피, 도서, 영화, AI 툴 등 분야별로 구글이 집계한 연도별 인기 검색어 순위.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900125, 3, 18, true, true, true, 0, 'Google 올해의 검색어', now(), now());

-- #145 (트렌드) -> seed_trend | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900126, 12, 1, 'https://brunch.co.kr/@vigorous21/1094', '트렌드에 민감하지 않아도 돼요', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900126, 900126, '트렌드에 민감하지 않아도 괜찮다며 트렌드를 좇지 않게 된 이유를 담은 코치의 에세이.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900126, 3, 21, true, true, true, 0, '트렌드에 민감하지 않아도 돼요', now(), now());

-- #146 (트렌드) -> seed_trend | www.vogue.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900127, 12, 1, 'https://www.vogue.co.kr/2025/08/13/2025년-가을에-유행할-8가지-바지-트렌드/', '2025년 가을에 유행할 8가지 바지 트렌드', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900127, 900127, '2025년 가을에 유행할 바지 스타일 8가지를 정리한 패션 트렌드 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900127, 3, 18, true, true, true, 0, '2025년 가을에 유행할 8가지 바지 트렌드', now(), now());

-- #147 (트렌드) -> seed_trend | heypop.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900128, 12, 1, 'https://heypop.kr/n/108232/', '2025년 떠오를 소비 트렌드는?', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900128, 900128, '2025년 떠오를 소비 트렌드와 관련한 팝업·전시 공간들을 소개하는 뉴스레터 콘텐츠.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900128, 3, 21, true, true, true, 0, '2025년 떠오를 소비 트렌드는?', now(), now());

-- #148 (트렌드) -> seed_trend | www.forbeskorea.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900129, 12, 1, 'https://www.forbeskorea.co.kr/news/articleView.html?idxno=400439', '케이글로잉, 2025 아마존 프라임데이 분석...''화장품 성분 중심 소비 트렌드 부상'' - 포브스코리아(Forbes Korea)', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900129, 900129, '2025년 아마존 프라임데이 분석을 통해 화장품 성분 중심 소비 트렌드 부상을 짚은 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900129, 3, 18, true, true, true, 0, '케이글로잉, 2025 아마존 프라임데이 분석...''화장품 성분 중심 소비 트렌드 부상'' - 포브스코리아(Forbes Korea)', now(), now());

-- #149 (트렌드) -> seed_trend | www.criteo.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900130, 12, 1, 'https://www.criteo.com/kr/blog/how-cost-conscious-travelers-are-reshaping-travel/', '2025년 상반기 글로벌 여행 트렌드 리포트 : 가격에 민감해진 여행자들이 만들어가는 새로운 트렌드 | KR - Criteo Global Travel Industry Trends 2025 H1 Q2 Q3 Free Report', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900130, 900130, '가격에 민감해진 여행자들이 만들어가는 2025년 상반기 글로벌 여행 트렌드 리포트.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900130, 3, 21, true, true, true, 0, '2025년 상반기 글로벌 여행 트렌드 리포트 : 가격에 민감해진 여행자들이 만들어가는 새로운 트렌드 | KR - Criteo Global Travel Industry Trends 2025 H1 Q2 Q3 Free Report', now(), now());

-- #150 (트렌드) -> seed_trend | trendmonitor.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900131, 12, 1, 'https://trendmonitor.co.kr/tmweb/trend/allTrend/detail.do?bIdx=1272&code=0404&trendType=CKOREA', '자고 일어나면 ‘상전벽해’인 현대사회에서 ''트렌드''를 쫓는 것은 살아남기 위한 생존', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900131, 900131, '빠르게 변하는 현대사회에서 트렌드를 좇는 것이 생존 전략이라는 점을 짚은 조사 보고서.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900131, 3, 18, true, true, true, 0, '자고 일어나면 ‘상전벽해’인 현대사회에서 ''트렌드''를 쫓는 것은 살아남기 위한 생존', now(), now());

-- #151 (트렌드) -> seed_trend | kbthink.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900132, 12, 1, 'https://kbthink.com/dictionary/view.html?dictId=KED-00015513', '트렌드란? - 뜻 & 정의 | KB의 생각', 0, 3, 21, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900132, 900132, '일정 기간 소비자들이 동조하는 변화된 소비 가치를 뜻하는 ''트렌드''의 정의를 설명한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900132, 3, 21, true, true, true, 0, '트렌드란? - 뜻 & 정의 | KB의 생각', now(), now());

-- #152 (트렌드) -> seed_trend | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900133, 12, 1, 'https://brunch.co.kr/@skyopqw/29', '빅데이터, 트렌드분석에 필요한 유용한 사이트', 0, 3, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900133, 900133, '대학 과제나 사업에 활용할 수 있는 빅데이터·트렌드 분석 유용 사이트들을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (110, 900133, 3, 18, true, true, true, 0, '빅데이터, 트렌드분석에 필요한 유용한 사이트', now(), now());

-- #153 (디자인·예술) -> seed_media_culture | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900134, 13, 1, 'https://brunch.co.kr/@onebirdme/24', '[생각하는 인간] 예술의 가치', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900134, 900134, '작품을 오감으로만 볼 때와 물음을 더해 느낄 때의 차이를 통해 예술의 가치를 짚은 에세이.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900134, 2, 18, true, true, true, 0, '[생각하는 인간] 예술의 가치', now(), now());

-- #154 (디자인·예술) -> seed_media_culture | www.kbergennews.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900135, 13, 1, 'https://www.kbergennews.com/2023/08/01/아트-현대미술이-왜-비싼가요/', '[아트] 현대미술이 왜 비싼가요?', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900135, 900135, '선 하나로 수백억원에 거래되는 현대미술이 왜 비싼지를 설명하는 전문가 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900135, 2, 23, true, true, true, 0, '[아트] 현대미술이 왜 비싼가요?', now(), now());

-- #156 (디자인·예술) -> seed_media_culture | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900136, 13, 1, 'https://brunch.co.kr/@nyeric/33', 'UX 디자인이란 무엇일까?', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900136, 900136, 'UX디자인이 무엇인지 현업 기획자가 자신의 경험을 바탕으로 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900136, 2, 18, true, true, true, 0, 'UX 디자인이란 무엇일까?', now(), now());

-- #160 (디자인·예술) -> seed_media_culture | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900137, 13, 1, 'https://brunch.co.kr/@miro0912/12', '뉴욕 현대미술관 (MoMA) #1', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900137, 900137, '뉴욕 현대미술관(MoMA)을 다녀온 뒤 남긴 관람기 시리즈의 첫 번째 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900137, 2, 23, true, true, true, 0, '뉴욕 현대미술관 (MoMA) #1', now(), now());

-- #162 (디자인·예술) -> seed_media_culture | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900138, 13, 1, 'https://brunch.co.kr/@bidpiece/26', '현대미술이 난해하고 비싼 이유', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900138, 900138, '테이프로 붙인 바나나처럼 난해해 보이는 현대미술 작품들이 왜 비싸게 팔리는지 설명한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900138, 2, 18, true, true, true, 0, '현대미술이 난해하고 비싼 이유', now(), now());

-- #166 (영상·뮤직) -> seed_media_culture | cmin.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900139, 14, 1, 'https://cmin.tistory.com/34', '바이올린 잘하고 싶어요! 바이올린 잘하는 법? -  1편', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900139, 900139, '좋은 바이올린 연주를 판단하는 기준과 잘하는 법을 다룬 연재 글의 1편.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900139, 2, 23, true, true, true, 0, '바이올린 잘하고 싶어요! 바이올린 잘하는 법? -  1편', now(), now());

-- #167 (영상·뮤직) -> seed_media_culture | ko.wikipedia.org
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900140, 14, 1, 'https://ko.wikipedia.org/wiki/베를린_필하모니_관현악단', '베를린 필하모니 관현악단 - 위키백과, 우리 모두의 백과사전', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900140, 900140, '독일 베를린을 기반으로 하는 세계적 오케스트라 베를린 필하모니 관현악단을 소개한 백과사전 문서.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900140, 2, 18, true, true, true, 0, '베를린 필하모니 관현악단 - 위키백과, 우리 모두의 백과사전', now(), now());

-- #170 (영상·뮤직) -> seed_media_culture | pitchfork.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900141, 14, 1, 'https://pitchfork.com/news/taylor-swift-reveals-new-album-cover-and-tracklist-on-travis-kelces-podcast-watch/', 'Taylor Swift Reveals New Album Cover, Tracklist, and Release Date on Travis Kelce’s Podcast', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900141, 900141, '테일러 스위프트가 트래비스 켈시의 팟캐스트에서 새 앨범 커버와 발매일을 공개했다는 소식.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900141, 2, 23, true, true, true, 0, 'Taylor Swift Reveals New Album Cover, Tracklist, and Release Date on Travis Kelce’s Podcast', now(), now());

-- #171 (영상·뮤직) -> seed_media_culture | kr.cyberlink.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900142, 14, 1, 'https://kr.cyberlink.com/blog/youtube-video-editing/215/youtube-video-editing', '유튜브 영상 편집하기 - 초보자를 위한 영상 편집 팁', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900142, 900142, '초보자를 위한 유튜브 영상 편집 기본 팁을 정리한 가이드 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900142, 2, 18, true, true, true, 0, '유튜브 영상 편집하기 - 초보자를 위한 영상 편집 팁', now(), now());

-- #175 (영상·뮤직) -> seed_media_culture | elysianeye.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900143, 14, 1, 'https://elysianeye.tistory.com/entry/음악을-감상하는-방법이-따로-있는가-전문-연주자들이-음악을-감상하는-방법이-따로-있는가-음악-감상법-에-대해서-알아보자', '음악을 감상하는 방법이 따로 있는가? 전문 연주자들이 음악을 감상하는 방법이 따로 있는가? 음악 감상법 에 대해서 알아보자.', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900143, 900143, '전문 연주자들은 음악을 어떻게 감상하는지, 음악 감상법에 대해 다룬 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900143, 2, 23, true, true, true, 0, '음악을 감상하는 방법이 따로 있는가? 전문 연주자들이 음악을 감상하는 방법이 따로 있는가? 음악 감상법 에 대해서 알아보자.', now(), now());

-- #177 (영상·뮤직) -> seed_media_culture | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900144, 14, 1, 'https://m.blog.naver.com/eureka_plus/221354177730', '[음악칼럼] ''록은 죽었다''', 0, 2, 18, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900144, 900144, '밴드 음악의 전성기는 지났지만 사람들은 여전히 음악을 즐길 것이라는 음악 칼럼.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (113, 900144, 2, 18, true, true, true, 0, '[음악칼럼] ''록은 죽었다''', now(), now());

-- #182 (맛집·여행) -> seed_travel_food | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900145, 15, 1, 'https://m.blog.naver.com/PostView.naver?blogId=kanonrei&logNo=223038035862&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '뉴욕 N달 살기 스타트', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900145, 900145, '미국병에 걸려 갑작스레 뉴욕에서 몇 달간 지내기로 하고 준비 과정을 남긴 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900145, 3, 28, true, true, true, 0, '뉴욕 N달 살기 스타트', now(), now());

-- #183 (맛집·여행) -> seed_travel_food | www.onda.me
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900146, 15, 1, 'https://www.onda.me/blog/yojeum-ddeuneun-gugnae-gwangwangji-6goseun', 'ONDA(온다) Blog | 요즘 뜨는 국내 관광지 6곳은?', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900146, 900146, '빅데이터로 분석한 2025년에 주목해야 할 국내 여행지 6곳을 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900146, 3, 29, true, true, true, 0, 'ONDA(온다) Blog | 요즘 뜨는 국내 관광지 6곳은?', now(), now());

-- #184 (맛집·여행) -> seed_travel_food | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900147, 15, 1, 'https://m.blog.naver.com/latcocho/222980324725', '[합정/맛집] TAO 마라탕 (타오 마라탕) _ 깔끔한 마라탕 찐맛집', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900147, 900147, '합정역 근처 깔끔한 마라탕 맛집 TAO 마라탕의 방문 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900147, 3, 28, true, true, true, 0, '[합정/맛집] TAO 마라탕 (타오 마라탕) _ 깔끔한 마라탕 찐맛집', now(), now());

-- #185 (맛집·여행) -> seed_travel_food | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900148, 15, 1, 'https://m.blog.naver.com/PostView.naver?blogId=soyjinny&logNo=223018449266&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '[미국 여행] 미국 필수 쇼핑리스트 / 미국에서 꼭 사와야 할 것 BEST', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900148, 900148, '미국 여행에서 꼭 사와야 할 필수 쇼핑 리스트를 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900148, 3, 29, true, true, true, 0, '[미국 여행] 미국 필수 쇼핑리스트 / 미국에서 꼭 사와야 할 것 BEST', now(), now());

-- #186 (맛집·여행) -> seed_travel_food | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900149, 15, 1, 'https://m.blog.naver.com/PostView.naver?blogId=jungdkfma&logNo=223503772081&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '간사이공항 교토 오사카 하루카 티켓 예약 및 교환 시간표, 지정석 발권 방법', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900149, 900149, '간사이공항과 교토·오사카를 잇는 하루카 특급열차 티켓 예약과 발권 방법을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900149, 3, 28, true, true, true, 0, '간사이공항 교토 오사카 하루카 티켓 예약 및 교환 시간표, 지정석 발권 방법', now(), now());

-- #189 (맛집·여행) -> seed_travel_food | www.tripplus.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900150, 15, 1, 'https://www.tripplus.co.kr/oversea/article/179905/', '2025년 가장 가고 싶은 해외여행지 1위 일본…2위는 동남아 아닌 이곳 - 여행플러스', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900150, 900150, '설문조사를 통해 본 2025년 가장 가고 싶은 해외여행지 순위, 1위는 일본이라는 기사.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900150, 3, 29, true, true, true, 0, '2025년 가장 가고 싶은 해외여행지 1위 일본…2위는 동남아 아닌 이곳 - 여행플러스', now(), now());

-- #190 (맛집·여행) -> seed_travel_food | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900151, 15, 1, 'https://m.blog.naver.com/apocalypseo/222230999966', '합정 맛집 리스트 20개', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900151, 900151, '합정역 도보 10분 내외에서 즐길 수 있는 맛집 20곳을 정리한 리스트.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900151, 3, 28, true, true, true, 0, '합정 맛집 리스트 20개', now(), now());

-- #192 (맛집·여행) -> seed_travel_food | mbiz.heraldcorp.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900152, 15, 1, 'https://mbiz.heraldcorp.com/article/10553905', '기차에서 먼저 만나는 안동 미식 여행…‘K-미식 전통주 벨트 팝업열차’ 출시 - 헤럴드경제', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900152, 900152, '기차 여행으로 안동의 전통주와 미식을 즐기는 팝업열차 상품 출시 소식.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900152, 3, 29, true, true, true, 0, '기차에서 먼저 만나는 안동 미식 여행…‘K-미식 전통주 벨트 팝업열차’ 출시 - 헤럴드경제', now(), now());

-- #193 (맛집·여행) -> seed_travel_food | codingnyeo.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900153, 15, 1, 'https://codingnyeo.tistory.com/m/entry/논산-여행-논산-가볼만한-곳-탑정호-출렁다리-레이크힐-제빵소-베이커리-카페-강경-가볼만한-곳', '논산 여행 논산 가볼만한 곳 탑정호 출렁다리 레이크힐 제빵소 베이커리 카페 강경 가볼만한 곳', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900153, 900153, '탑정호 출렁다리 등 논산에서 가볼 만한 여행지와 베이커리 카페를 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900153, 3, 28, true, true, true, 0, '논산 여행 논산 가볼만한 곳 탑정호 출렁다리 레이크힐 제빵소 베이커리 카페 강경 가볼만한 곳', now(), now());

-- #194 (맛집·여행) -> seed_travel_food | jiho9249.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900154, 15, 1, 'https://jiho9249.tistory.com/entry/일산-☕-스퀘어81-–-일산에서-늦게까지-즐기는-감성-카페-🌙', '[일산] ☕ 스퀘어81 – 일산에서 늦게까지 즐기는 감성 카페 🌙', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900154, 900154, '새벽까지 영업하는 일산의 감성 카페 ''스퀘어81'' 방문 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900154, 3, 29, true, true, true, 0, '[일산] ☕ 스퀘어81 – 일산에서 늦게까지 즐기는 감성 카페 🌙', now(), now());

-- #195 (맛집·여행) -> seed_travel_food | brunch.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900155, 15, 1, 'https://brunch.co.kr/@saddysb/3', '즐길거리 가득한 뉴욕 여행', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900155, 900155, '여유를 위해 항공 크레딧을 활용해 떠난 뉴욕 여행기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900155, 3, 28, true, true, true, 0, '즐길거리 가득한 뉴욕 여행', now(), now());

-- #196 (맛집·여행) -> seed_travel_food | woni-history.tistory.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900156, 15, 1, 'https://woni-history.tistory.com/entry/미국동부-뉴욕여행맨해튼에서-꼭-해봐야-할-12가지뉴욕-맨해튼-여행-후기-및-꿀팁', '[미국 동부 뉴욕 여행] 맨해튼에서 꼭 해 봐야 할 12가지 / 뉴욕 맨해튼 여행 후기 및 꿀팁', 0, 3, 29, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900156, 900156, '뉴욕 맨해튼 여행에서 꼭 해봐야 할 12가지와 여행 팁을 정리한 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900156, 3, 29, true, true, true, 0, '[미국 동부 뉴욕 여행] 맨해튼에서 꼭 해 봐야 할 12가지 / 뉴욕 맨해튼 여행 후기 및 꿀팁', now(), now());

-- #200 (맛집·여행) -> seed_travel_food | www.towncar.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900157, 15, 1, 'https://www.towncar.co.kr/post/the-3-best-drives-in-the-suburbs', '훌쩍 떠나기 좋은 경기도 드라이브 코스 BEST 3!', 0, 3, 28, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900157, 900157, '훌쩍 떠나기 좋은 경기도 드라이브 코스 3곳을 추천한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (111, 900157, 3, 28, true, true, true, 0, '훌쩍 떠나기 좋은 경기도 드라이브 코스 BEST 3!', now(), now());

-- #203 (기타) -> seed_etc | www.disneyplus.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900158, 16, 1, 'https://www.disneyplus.com/ko-kr', '무궁무진한 영화, TV 시리즈, 오리지널 | 디즈니+', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900158, 900158, '디즈니+에서 스트리밍 중인 신작 콘텐츠들과 티빙·웨이브 번들 요금제를 소개한 안내.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900158, 2, 23, true, true, true, 0, '무궁무진한 영화, TV 시리즈, 오리지널 | 디즈니+', now(), now());

-- #204 (기타) -> seed_etc | www.ppomppu.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900159, 16, 1, 'https://www.ppomppu.co.kr/zboard/view.php?id=movie&no=87077', '케데헌 떼창 공부하는 레딧 ㄷㄷ.jpg', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900159, 900159, '''케이팝 데몬 헌터스'' 노래 가사를 따라 부르며 공부하는 외국 커뮤니티 반응을 다룬 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900159, 2, 19, true, true, true, 0, '케데헌 떼창 공부하는 레딧 ㄷㄷ.jpg', now(), now());

-- #206 (기타) -> seed_etc | ppomppu.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900160, 16, 1, 'https://ppomppu.co.kr/zboard/view.php?id=gamer&page=1&divpage=11&no=57890', '플스 포탈 딜레이 어느정도인가요?', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900160, 900160, '플레이스테이션 포탈의 클라우드 게이밍 딜레이 체감에 대해 묻고 답한 커뮤니티 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900160, 2, 23, true, true, true, 0, '플스 포탈 딜레이 어느정도인가요?', now(), now());

-- #207 (기타) -> seed_etc | www.archives.gov
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900161, 16, 1, 'https://www.archives.gov/research/mlk', 'Records Related to the Assassination of the Reverend Dr. Martin Luther King, Jr.', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900161, 900161, '마틴 루터 킹 목사 암살 관련 미국 국가기록원의 공개 문서 목록을 안내하는 페이지.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900161, 2, 19, true, true, true, 0, 'Records Related to the Assassination of the Reverend Dr. Martin Luther King, Jr.', now(), now());

-- #210 (기타) -> seed_etc | about.kbs.co.kr
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900162, 16, 1, 'https://about.kbs.co.kr/index.html?sname=report&stype=innovation', 'About KBS', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900162, 900162, 'KBS 이사회가 방송법에 따라 구성한 경영평가단의 연간 경영 평가 결과를 안내하는 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900162, 2, 23, true, true, true, 0, 'About KBS', now(), now());

-- #211 (기타) -> seed_etc | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900163, 16, 1, 'https://m.blog.naver.com/PostView.naver?blogId=ful0907&logNo=222246442693&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', 'cordovan wallet 코도반 남성 반지갑 주문제작,가죽공예일일 클래스 핸드메이드 잠실 가죽공방 언블런', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900163, 900163, '잠실 가죽공방에서 코도반 소재로 남성 반지갑을 주문 제작한 후기와 원데이 클래스 소개.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900163, 2, 19, true, true, true, 0, 'cordovan wallet 코도반 남성 반지갑 주문제작,가죽공예일일 클래스 핸드메이드 잠실 가죽공방 언블런', now(), now());

-- #212 (기타) -> seed_etc | m.kin.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900164, 16, 1, 'https://m.kin.naver.com/mobile/qna/detail.naver?d1Id=1&dirId=10303&docId=446086437', '맥북 오류', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900164, 900164, '초기화 도중 멈춘 맥북을 정상적으로 복구하는 방법을 묻고 답한 Q&A 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900164, 2, 23, true, true, true, 0, '맥북 오류', now(), now());

-- #213 (기타) -> seed_etc | kr.imyfone.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900165, 16, 1, 'https://kr.imyfone.com/ios-data-recovery/how-to-recover-deleted-photo/', '아이폰 삭제 된 사진 복구 성공률 높이는 3가지 팁', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900165, 900165, '실수로 삭제되거나 iOS 업데이트 오류로 사라진 아이폰 사진을 복구하는 방법 네 가지를 소개한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900165, 2, 19, true, true, true, 0, '아이폰 삭제 된 사진 복구 성공률 높이는 3가지 팁', now(), now());

-- #214 (기타) -> seed_etc | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900166, 16, 1, 'https://m.blog.naver.com/PostView.naver?blogId=tagheuer1130&logNo=222185945489&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '아크네스튜디오 페어뷰 맨투맨 옐로우 구매 후기 & 사이즈 리뷰 (Feat. 아크네스튜디오 포바 비교)', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900166, 900166, '아크네스튜디오 페어뷰 맨투맨을 구매하고 포바 제품과 비교한 사이즈·핏 후기.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900166, 2, 23, true, true, true, 0, '아크네스튜디오 페어뷰 맨투맨 옐로우 구매 후기 & 사이즈 리뷰 (Feat. 아크네스튜디오 포바 비교)', now(), now());

-- #215 (기타) -> seed_etc | m.blog.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900167, 16, 1, 'https://m.blog.naver.com/PostView.naver?blogId=jylking123&logNo=222972127575&proxyReferer=https:%2F%2Fm.keep.naver.com%2F&trackingCode=naver_etc', '유럽 배낭여행 :: 해외여행 필수 준비물 (베드버그 퇴치제 , 소매치기 방지용품 !)', 0, 2, 19, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900167, 900167, '유럽 배낭여행을 떠날 때 베드버그 퇴치제, 소매치기 방지용품 등 챙겨야 할 준비물을 정리한 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900167, 2, 19, true, true, true, 0, '유럽 배낭여행 :: 해외여행 필수 준비물 (베드버그 퇴치제 , 소매치기 방지용품 !)', now(), now());

-- #216 (기타) -> seed_etc | m.kin.naver.com
INSERT INTO linkus (linku_id, category_id, domain_id, linku_url, title, total_view_count, emotion_id, situation_id, created_at, updated_at) VALUES (900168, 16, 1, 'https://m.kin.naver.com/mobile/qna/detail.naver?d1Id=4&dirId=40402&docId=370013546', '회계 분개 문제 부탁드립니다 ㅠㅠ', 0, 2, 23, now(), now()) ON CONFLICT (linku_id) DO NOTHING;
INSERT INTO ai_articles (ai_article_id, linku_id, summary, created_at, updated_at) VALUES (900168, 900168, '특정 회사의 3월 거래내역을 바탕으로 한 회계 분개 문제 풀이를 요청하는 질문 글.', now(), now()) ON CONFLICT (ai_article_id) DO NOTHING;
INSERT INTO users_linkus (user_id, linku_id, emotion_id, situation_id, is_emotion_ai, is_situation_ai, is_ai_exist, view_count, title, created_at, updated_at) VALUES (112, 900168, 2, 23, true, true, true, 0, '회계 분개 문제 부탁드립니다 ㅠㅠ', now(), now());

SELECT setval(pg_get_serial_sequence('linkus', 'linku_id'), 900168, true);
SELECT setval(pg_get_serial_sequence('ai_articles', 'ai_article_id'), 900168, true);
