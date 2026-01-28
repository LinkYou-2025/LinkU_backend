-- domain 데이터 삽입 (crawl_strategy 컬럼 추가 포함)
INSERT INTO domain (domain_id, domain_tail, image_url, name, crawl_strategy) VALUES
     (1, 'invalid', NULL, 'invalid', 'DEFAULT'),
     (2, 'blog.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/416766de-7e9c-4033-9d08-cf56a90f85c3.png', 'blog.naver', 'IFRAME'),
     (3, 'cafe.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/382fd711-bc04-4185-8174-cff8546ec85e.png', 'cafe.naver', 'DEFAULT'),  -- iframe 불확실, 기본 직접 크롤링
     (4, 'kin.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/a2204494-8f68-41c5-8fc9-494f0328ed64.png', 'kin.naver', 'IFRAME'),
     (5, 'shopping.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/32d11801-e291-436e-9c2a-e755d2517d20.png', 'shopping.naver', 'DEFAULT'),
     (6, 'github.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/a5bfcf0c-be33-4c18-9773-c75601806feb.png', 'github', 'DEFAULT'),
     (7, 'linkedin.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/3eb0fb2b-3edb-41c9-a256-3f9dfe19ce48.png', 'linkedin', 'DEFAULT'),
     (8, 'tistory.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/e603a485-e147-46fc-8831-0064604b7868.png', 'tistory', 'DEFAULT'),
     (9, 'google.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/222e6458-2bbd-47a1-8154-c911b059cc89.png', 'google', 'DEFAULT'),
     (10, 'nytimes.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/0d02a4f3-f447-426f-8dfc-803973507f3d.png', 'nytimes', 'DEFAULT'),
     (11, 'brunch.co.kr', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/d6adf9f7-344c-4cec-8cac-7e64f12d8af0.png', 'brunch', 'DEFAULT'),
     (12, 'velog.io', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/ef86fa27-2b0a-421b-b51c-ee881cf45cf7.png', 'velog', 'DEFAULT'),
     (13, 'daum.net', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/7d1e7dc0-36dd-4b9e-a593-e9a6045d2dca.png', 'daum', 'DEFAULT'),
     (14, 'jobkorea.co.kr', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/d56fa67f-d64e-4f88-a159-c6f421297a50.png', 'jobkorea', 'DEFAULT'),
     (15, 'wanted.co.kr', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/dd7b199e-818f-4118-8410-637d15100df2.png', 'wanted', 'DEFAULT'),
     (16, 'musinsa.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/cb230106-0d22-4488-ac45-bac1546a6fa2.png', 'musinsa', 'DEFAULT'),
     (17, '11st.co.kr', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/df7f17b8-80b3-4cb3-96bb-5c5048953ed7.png', '11st', 'DEFAULT'),
     (18, 'instagram.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/d4906426-cf6e-4480-8b06-009167720b7f.png', 'instagram', 'DEFAULT'),
     (19, 'twitter.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/e07e2ced-7f1c-4a3d-a545-04066eced0e0.png', 'twitter', 'DEFAULT'),
     (20, 'facebook.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/91eb63c2-46b1-46f0-a28c-449fb567b408.png', 'facebook', 'DEFAULT'),
     (21, 'naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver', 'IFRAME'),
     (22, 'm.blog.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/416766de-7e9c-4033-9d08-cf56a90f85c3.png', 'blog.naver', 'IFRAME'),
     (23, 'blogon.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/416766de-7e9c-4033-9d08-cf56a90f85c3.png', 'blog.naver', 'IFRAME'),
     (24, 'm.cafe.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/382fd711-bc04-4185-8174-cff8546ec85e.png', 'cafe.naver', 'DEFAULT'),
     (25, 'cafeon.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/382fd711-bc04-4185-8174-cff8546ec85e.png', 'cafe.naver', 'DEFAULT'),
     (26, 'news.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver news', 'DEFAULT'),
     (27, 'n.news.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver news', 'DEFAULT'),
     (28, 'post.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver post', 'IFRAME'),
     (29, 'm.post.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver post', 'IFRAME'),
     (30, 'm.shopping.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/32d11801-e291-436e-9c2a-e755d2517d20.png', 'shopping.naver', 'DEFAULT'),
     (31, 'smartstore.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/32d11801-e291-436e-9c2a-e755d2517d20.png', 'shopping.naver', 'DEFAULT'),
     (32, 'm.kin.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/a2204494-8f68-41c5-8fc9-494f0328ed64.png', 'kin.naver', 'IFRAME'),
     (33, 'map.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver map', 'DEFAULT'),
     (34, 'm.map.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver map', 'DEFAULT'),
     (35, 'comic.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver comic', 'DEFAULT'),
     (36, 'm.comic.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver comic', 'DEFAULT'),
     (37, 'naver.me', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver shortlink', 'DEFAULT'),
     (38, 'dict.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver dict', 'DEFAULT'),
     (39, 'ko.dict.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver dict', 'DEFAULT'),
     (40, 'en.dict.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver dict', 'DEFAULT'),
     (41, 'newsstand.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver newsstand', 'DEFAULT'),
     (42, 'm.newsstandnaver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver newsstand', 'DEFAULT'),
     (43, 'novel.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver novel', 'DEFAULT'),
     (44, 'series.naver.com', 'https://d3f9zmi4jicqrs.cloudfront.net/domain/81d40a80-1500-422f-b8c4-6748d3a55ca3.png', 'naver series', 'DEFAULT'),
     (45, 'tv.naver.com', NULL, 'tv.naver', 'DEFAULT'),
     (46, 'x.com', NULL, 'x', 'DEFAULT'),
     (47, 'youtube.com', NULL, 'youtube', 'DEFAULT'),
     (48, 'tiktok.com', NULL, 'tiktok', 'DEFAULT'),
     (49, 'threads.net', NULL, 'threads', 'DEFAULT'),
     (50, 'band.us', NULL, 'band', 'DEFAULT'),
     (51, 'story.kakao.com', NULL, 'kakaostory', 'DEFAULT'),
     (52, 'beige.world', NULL, 'beige', 'DEFAULT'),
     (53, 'magazine-b.com', NULL, 'magazineb', 'DEFAULT'),
     (54, 'indiepost.co.kr', NULL, 'indiepost', 'DEFAULT'),
     (55, 'seoulstore.com', NULL, 'seoulstore', 'DEFAULT'),
     (56, 'the-edit.co.kr', NULL, 'the-edit', 'DEFAULT'),
     (57, 'vice.com', NULL, 'vice', 'DEFAULT'),
     (58, 'hypebeast.kr', NULL, 'hypebeast', 'DEFAULT'),
     (59, 'hankookilbo.com', NULL, 'hankookilbo', 'DEFAULT'),
     (60, 'joongang.co.kr', NULL, 'joongang', 'DEFAULT'),
     (61, 'hankyung.com', NULL, 'hankyung', 'DEFAULT'),
     (62, 'chosun.com', NULL, 'chosun', 'DEFAULT'),
     (63, 'khan.co.kr', NULL, 'khan', 'DEFAULT'),
     (64, 'ohmynews.com', NULL, 'ohmynews', 'DEFAULT'),
     (65, 'pressian.com', NULL, 'pressian', 'DEFAULT'),
     (66, 'dailysecu.com', NULL, 'dailysecu', 'DEFAULT'),
     (67, 'zdnet.co.kr', NULL, 'zdnet', 'DEFAULT'),
     (68, 'bloter.net', NULL, 'bloter', 'DEFAULT'),
     (69, 'itworld.co.kr', NULL, 'itworld', 'DEFAULT'),
     (70, 'platum.kr', NULL, 'platum', 'DEFAULT'),
     (71, 'venturesquare.net', NULL, 'venturesquare', 'DEFAULT'),
     (72, 'startuptoday.co.kr', NULL, 'startuptoday', 'DEFAULT'),
     (73, 'fntimes.com', NULL, 'fntimes', 'DEFAULT'),
     (74, 'mk.co.kr', NULL, 'mk', 'DEFAULT'),
     (75, 'yna.co.kr', NULL, 'yonhapnews', 'DEFAULT'),
     (76, 'newsis.com', NULL, 'newsis', 'DEFAULT'),
     (77, 'biz.heraldcorp.com', NULL, 'heraldcorp', 'DEFAULT'),
     (78, 'donga.com', NULL, 'donga', 'DEFAULT'),
     (79, 'cjenm.com', NULL, 'cjenm', 'DEFAULT'),
     (80, 'tvn.cjenm.com', NULL, 'tvn', 'DEFAULT'),
     (81, 'tving.com', NULL, 'tving', 'DEFAULT'),
     (82, 'wavve.com', NULL, 'wavve', 'DEFAULT'),
     (83, 'seezn.com', NULL, 'seezn', 'DEFAULT'),
     (84, 'melon.com', NULL, 'melon', 'DEFAULT'),
     (85, 'genie.co.kr', NULL, 'genie', 'DEFAULT'),
     (86, 'bugs.co.kr', NULL, 'bugs', 'DEFAULT'),
     (87, 'music-flo.com', NULL, 'flo', 'DEFAULT'),
     (88, 'vlive.tv', NULL, 'vlive', 'DEFAULT'),
     (89, 'pannchoa.blogspot.com', NULL, 'pannchoa', 'DEFAULT'),
     (90, 'netmarble.net', NULL, 'netmarble', 'DEFAULT'),
     (91, 'nexon.com', NULL, 'nexon', 'DEFAULT'),
     (92, 'krafton.com', NULL, 'krafton', 'DEFAULT'),
     (93, 'smilegate.com', NULL, 'smilegate', 'DEFAULT'),
     (94, 'riotgames.com', NULL, 'riotgames', 'DEFAULT'),
     (95, 'afreecatv.com', NULL, 'afreecatv', 'DEFAULT'),
     (96, 'twitch.tv', NULL, 'twitch', 'DEFAULT'),
     (97, 'fastcampus.co.kr', NULL, 'fastcampus', 'DEFAULT'),
     (98, 'inflearn.com', NULL, 'inflearn', 'DEFAULT'),
     (99, 'class101.net', NULL, 'class101', 'DEFAULT'),
     (100, 'coloso.co.kr', NULL, 'coloso', 'DEFAULT'),
     (101, 'modu.ac.kr', NULL, 'modu', 'DEFAULT'),
     (102, 'koreatech.ac.kr', NULL, 'koreatech', 'DEFAULT'),
     (103, 'seoultech.ac.kr', NULL, 'seoultech', 'DEFAULT'),
     (104, 'kmooc.kr', NULL, 'kmooc', 'DEFAULT'),
     (105, 'tedxseoul.com', NULL, 'tedxseoul', 'DEFAULT'),
     (106, 'coupangplay.com', NULL, 'coupangplay', 'DEFAULT'),
     (107, 'watcha.com', NULL, 'watcha', 'DEFAULT'),
     (108, 'netflix.com', NULL, 'netflix', 'DEFAULT'),
     (109, 'disneyplus.com', NULL, 'disneyplus', 'DEFAULT'),
     (110, 'spotvnews.co.kr', NULL, 'spotvnews', 'DEFAULT'),
     (111, 'sportsseoul.com', NULL, 'sportsseoul', 'DEFAULT'),
     (112, 'koreaherald.com', NULL, 'koreaherald', 'DEFAULT'),
     (113, 'saramin.co.kr', NULL, 'saramin', 'DEFAULT'),
     (114, 'rocketpunch.com', NULL, 'rocketpunch', 'DEFAULT'),
     (115, 'peoplefund.co.kr', NULL, 'peoplefund', 'DEFAULT'),
     (116, 'toss.im', NULL, 'toss', 'DEFAULT'),
     (117, 'kakaobank.com', NULL, 'kakaobank', 'DEFAULT'),
     (118, 'kakaopay.com', NULL, 'kakaopay', 'DEFAULT'),
     (119, 'shinhancard.com', NULL, 'shinhancard', 'DEFAULT'),
     (120, 'wooribank.com', NULL, 'wooribank', 'DEFAULT'),
     (121, 'kbstar.com', NULL, 'kbbank', 'DEFAULT'),
     (122, 'kebhana.com', NULL, 'hana', 'DEFAULT'),
     (123, 'ibk.co.kr', NULL, 'ibk', 'DEFAULT'),
     (124, 'finnq.com', NULL, 'finnq', 'DEFAULT'),
     (125, 'bank.toss.im', NULL, 'tossbank', 'DEFAULT'),
     (126, 'nonghyup.com', NULL, 'nhbank', 'DEFAULT'),
     (127, 'busanbank.co.kr', NULL, 'busanbank', 'DEFAULT'),
     (128, 'jejubank.co.kr', NULL, 'jejubank', 'DEFAULT'),
     (129, 'finance.naver.com', NULL, 'finance.naver', 'DEFAULT'),
     (130, 'map.kakao.com', NULL, 'map.kakao', 'DEFAULT'),
     (131, 'molit.go.kr', NULL, 'molit', 'DEFAULT'),
     (132, 'moel.go.kr', NULL, 'moel', 'DEFAULT'),
     (133, 'kosis.kr', NULL, 'kosis', 'DEFAULT'),
     (134, 'data.go.kr', NULL, 'data', 'DEFAULT'),
     (135, 'seoul.go.kr', NULL, 'seoul', 'DEFAULT'),
     (136, 'airport.co.kr', NULL, 'airport', 'DEFAULT'),
     (137, 'koreaexim.go.kr', NULL, 'koreaexim', 'DEFAULT'),
     (138, 'immigration.go.kr', NULL, 'immigration', 'DEFAULT'),
     (139, 'korea.net', NULL, 'korea', 'DEFAULT'),
     (140, 'pinterest.com', NULL, 'pinterest', 'DEFAULT'),
     (141, 'reddit.com', NULL, 'reddit', 'DEFAULT'),
     (142, 'medium.com', NULL, 'medium', 'DEFAULT'),
     (143, 'substack.com', NULL, 'substack', 'DEFAULT'),
     (144, 'notion.so', NULL, 'notion', 'DEFAULT'),
     (145, 'monday.com', NULL, 'monday', 'DEFAULT'),
     (146, 'slack.com', NULL, 'slack', 'DEFAULT'),
     (147, 'discord.com', NULL, 'discord', 'DEFAULT'),
     (148, 'figma.com', NULL, 'figma', 'DEFAULT'),
     (149, 'canva.com', NULL, 'canva', 'DEFAULT'),
     (150, 'miro.com', NULL, 'miro', 'DEFAULT'),
     (151, 'trello.com', NULL, 'trello', 'DEFAULT'),
     (152, 'airtable.com', NULL, 'airtable', 'DEFAULT'),
     (153, 'asana.com', NULL, 'asana', 'DEFAULT'),
     (154, 'clickup.com', NULL, 'clickup', 'DEFAULT'),
     (155, 'linear.app', NULL, 'linear', 'DEFAULT'),
     (156, 'loom.com', NULL, 'loom', 'DEFAULT'),
     (157, 'typeform.com', NULL, 'typeform', 'DEFAULT'),
     (158, 'notionforms.io', NULL, 'notionforms', 'DEFAULT'),
     (159, 'super.so', NULL, 'super', 'DEFAULT'),
     (160, 'framer.com', NULL, 'framer', 'DEFAULT'),
     (161, 'webflow.com', NULL, 'webflow', 'DEFAULT'),
     (162, 'wix.com', NULL, 'wix', 'DEFAULT'),
     (163, 'wordpress.com', NULL, 'wordpress', 'DEFAULT'),
     (164, 'behance.net', NULL, 'behance', 'DEFAULT'),
     (165, 'dribbble.com', NULL, 'dribbble', 'DEFAULT'),
     (166, 'adobe.com', NULL, 'adobe', 'DEFAULT'),
     (167, 'figresources.com', NULL, 'figmaresources', 'DEFAULT'),
     (168, 'awwwards.com', NULL, 'awwwards', 'DEFAULT'),
     (169, 'producthunt.com', NULL, 'producthunt', 'DEFAULT'),
     (170, 'indiehackers.com', NULL, 'indiehackers', 'DEFAULT'),
     (171, 'dev.to', NULL, 'devto', 'DEFAULT'),
     (172, 'gitlab.com', NULL, 'gitlab', 'DEFAULT'),
     (173, 'stackoverflow.com', NULL, 'stackoverflow', 'DEFAULT'),
     (174, 'vercel.com', NULL, 'vercel', 'DEFAULT'),
     (175, 'netlify.com', NULL, 'netlify', 'DEFAULT'),
     (176, 'replit.com', NULL, 'replit', 'DEFAULT'),
     (177, 'openai.com', NULL, 'openai', 'DEFAULT'),
     (178, 'huggingface.co', NULL, 'huggingface', 'DEFAULT'),
     (179, 'kaggle.com', NULL, 'kaggle', 'DEFAULT'),
     (180, 'unsplash.com', NULL, 'unsplash', 'DEFAULT'),
     (181, 'pexels.com', NULL, 'pexels', 'DEFAULT'),
     (182, 'pixabay.com', NULL, 'pixabay', 'DEFAULT'),
     (183, 'remove.bg', NULL, 'removebg', 'DEFAULT'),
     (184, 'tinywow.com', NULL, 'tinywow', 'DEFAULT'),
     (185, 'flaticon.com', NULL, 'flaticon', 'DEFAULT'),
     (186, 'iconfinder.com', NULL, 'iconfinder', 'DEFAULT'),
     (187, 'fontshare.com', NULL, 'fontshare', 'DEFAULT'),
     (188, 'fontawesome.com', NULL, 'fontawesome', 'DEFAULT'),
     (189, 'coolors.co', NULL, 'coolors', 'DEFAULT'),
     (190, 'palettegenerator.com', NULL, 'palettegenerator', 'DEFAULT'),
     (191, 'brandcolors.net', NULL, 'brandcolors', 'DEFAULT'),
     (192, 'loomly.com', NULL, 'loomly', 'DEFAULT'),
     (193, 'buffer.com', NULL, 'buffer', 'DEFAULT'),
     (194, 'later.com', NULL, 'later', 'DEFAULT'),
     (195, 'hootsuite.com', NULL, 'hootsuite', 'DEFAULT'),
     (196, 'linktr.ee', NULL, 'linktr', 'DEFAULT'),
     (197, 'carrd.co', NULL, 'carrd', 'DEFAULT'),
     (198, 'bio.fm', NULL, 'biofms', 'DEFAULT'),
     (199, 'spotify.com', NULL, 'spotify', 'DEFAULT'),
     (200, 'music.apple.com', NULL, 'applemusic', 'DEFAULT'),
     (201, 'soundcloud.com', NULL, 'soundcloud', 'DEFAULT'),
     (202, 'bandcamp.com', NULL, 'bandcamp', 'DEFAULT'),
     (203, 'hulu.com', NULL, 'hulu', 'DEFAULT'),
     (204, 'primevideo.com', NULL, 'primevideo', 'DEFAULT'),
     (205, 'max.com', NULL, 'max', 'DEFAULT'),
     (206, 'music.youtube.com', NULL, 'youtube_music', 'DEFAULT'),
     (207, 'coursera.org', NULL, 'coursera', 'DEFAULT'),
     (208, 'udemy.com', NULL, 'udemy', 'DEFAULT'),
     (209, 'edx.org', NULL, 'edx', 'DEFAULT'),
     (210, 'skillshare.com', NULL, 'skillshare', 'DEFAULT'),
     (211, 'khanacademy.org', NULL, 'khanacademy', 'DEFAULT'),
     (212, 'futurelearn.com', NULL, 'futurelearn', 'DEFAULT'),
     (213, 'brilliant.org', NULL, 'brilliant', 'DEFAULT'),
     (214, 'duolingo.com', NULL, 'duolingo', 'DEFAULT'),
     (215, 'babbel.com', NULL, 'babbel', 'DEFAULT'),
     (216, 'quizlet.com', NULL, 'quizlet', 'DEFAULT'),
     (217, 'notionacademy.io', NULL, 'notionacademy', 'DEFAULT'),
     (218, 'ted.com', NULL, 'ted', 'DEFAULT'),
     (219, 'school.medium.com', NULL, 'mediumschool', 'DEFAULT'),
     (220, 'bbc.com', NULL, 'bbc', 'DEFAULT'),
     (221, 'cnn.com', NULL, 'cnn', 'DEFAULT'),
     (222, 'theguardian.com', NULL, 'theguardian', 'DEFAULT'),
     (223, 'wired.com', NULL, 'wired', 'DEFAULT'),
     (224, 'techcrunch.com', NULL, 'techcrunch', 'DEFAULT'),
     (225, 'theverge.com', NULL, 'theverge', 'DEFAULT'),
     (226, 'forbes.com', NULL, 'forbes', 'DEFAULT'),
     (227, 'bloomberg.com', NULL, 'bloomberg', 'DEFAULT'),
     (228, 'reuters.com', NULL, 'reuters', 'DEFAULT'),
     (229, 'businessinsider.com', NULL, 'businessinsider', 'DEFAULT'),
     (230, 'morningbrew.com', NULL, 'morningbrew', 'DEFAULT'),
     (231, 'hbr.org', NULL, 'hbr', 'DEFAULT'),
     (232, 'medium.design', NULL, 'mediumdesign', 'DEFAULT'),
     (233, 'creativebloq.com', NULL, 'creativebloq', 'DEFAULT'),
     (234, '99u.adobe.com', NULL, '99u', 'DEFAULT'),
     (235, 'mymodernmet.com', NULL, 'mymodernmet', 'DEFAULT'),
     (236, 'dezeen.com', NULL, 'dezeen', 'DEFAULT'),
     (237, 'archdaily.com', NULL, 'archdaily', 'DEFAULT'),
     (238, 'uxdesign.cc', NULL, 'uxdesign', 'DEFAULT'),
     (239, 'uxplanet.org', NULL, 'uxplanet', 'DEFAULT'),
     (240, 'smashingmagazine.com', NULL, 'smashingmagazine', 'DEFAULT'),
     (241, 'goodreads.com', NULL, 'goodreads', 'DEFAULT'),
     (242, 'letterboxd.com', NULL, 'letterboxd', 'DEFAULT'),
     (243, 'imdb.com', NULL, 'imdb', 'DEFAULT'),
     (244, 'rottentomatoes.com', NULL, 'rottentomatoes', 'DEFAULT')
    ON DUPLICATE KEY UPDATE domain_id = domain_id;


INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (1, '레드', '#FF5353', '#FFA0A0', '#FFC2C2', '#FFEEEE')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (2, '오렌지', '#FF6A2B', '#FFA783', '#FFD3C1', '#FFEBE1')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (3, '라이트 오렌지', '#FF9C2B', '#FFB867', '#FFD4A3', '#FFEFDD')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (4, '옐로', '#FFCE45', '#FFE291', '#FFEEBE', '#FFFAEB')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (5, '라이트 그린', '#77E61D', '#B8F785', '#DAFBD0', '#EEFFE0')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (6, '그린', '#00C774', '#6BDFAE', '#B8F0D9', '#DAF7EB')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (7, '딥그린', '#4B9857', '#70AA79', '#9ECCA6', '#C7E8CD')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (8, '민트', '#36D1BE', '#98EDE2', '#B9F7F0', '#DDFFFB')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (9, '라이트 블루', '#34BFFF', '#97D8FF', '#D0EFFF', '#ECF8FF')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (10, '블루', '#4C7AF8', '#82A3FF', '#C2D2FF', '#E6ECFF')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (11, '딥퍼플', '#813CFF', '#A778FF', '#C5A6FF', '#EDE4FF')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (12, '퍼플', '#BA5AFF', '#D8A3FF', '#E7C6FF', '#F4E6FF')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (13, '핑크', '#FF52DF', '#FF9FED', '#FFC7F5', '#FFEBFB')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (14, '딥핑크', '#FF459C', '#FFA2CC', '#FFC5E0', '#FFE7F2')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (15, '브라운', '#906744', '#BE9A7B', '#E0B795', '#F4E6DB')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);

INSERT INTO fcolor
(fcolor_id, color_name, color_code1, color_code2, color_code3, color_code4)
VALUES
    (16, '블랙', '#000000', '#747474', '#AFAFAF', '#D4D4D4')
ON DUPLICATE KEY UPDATE
                     color_name  = VALUES(color_name),
                     color_code1 = VALUES(color_code1),
                     color_code2 = VALUES(color_code2),
                     color_code3 = VALUES(color_code3),
                     color_code4 = VALUES(color_code4);


INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (1, 1, '어학')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (2, 2, '뉴스')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (3, 3, '공부법')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (4, 4, 'IT·개발')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (5, 5, '자기계발')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (6, 6, '취업·이직')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (7, 7, '비즈니스 인사이트')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (8, 8, '생산성·툴')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (9, 9, '라이프스타일')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (10, 10, '심리·자기이해')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (11, 11, '에세이·칼럼')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (12, 12, '트렌드')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (13, 13, '디자인·예술')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (14, 14, '영상·뮤직')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (15, 15, '맛집·여행')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);
INSERT INTO category (category_id, fcolor_id, category_name)
VALUES (16, 16, '기타')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name),
                        fcolor_id    = VALUES(fcolor_id);


INSERT INTO emotion (emotion_id, name)
VALUES (1, '즐거움')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name)
VALUES (2, '평온')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name)
VALUES (3, '설렘')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name)
VALUES (4, '슬픔')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name)
VALUES (5, '짜증')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name)
VALUES (6, '분노')
ON DUPLICATE KEY UPDATE name = VALUES(name);


-- 즐거움 (emotion_id = 1)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (1, '(닉네임)님, 요즘 기분 좋은 일이 많았죠? 그 흐름을 더 이어갈 콘텐츠들이에요.', '지금의 긍정적인 에너지를 놓치지 말고, 조금 더 넓혀봐요!')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (1, '(닉네임)님, 기분 좋을 때 찾는 콘텐츠는 기억에 오래 남아요. 오늘도 그런 하루가 되기를!', '마음이 열려 있을 때, 생각보다 더 많은 걸 받아들일 수 있거든요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (1, '(닉네임)님의 좋은 하루, 더 좋은 생각으로 채워볼까요? 지금 이 기분을 조금 더 확장해줄 콘텐츠들이에요.', '행복은 종종 아주 사소한 클릭 하나에서 시작되니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (1, '(닉네임)님, 기분 좋은 지금, 뭔가 새롭고 흥미로운 걸 만나보는 건 어때요?', '즐거움은 나누면 배가 되죠. 이 감정을 오래 기억할 수 있기를!')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

-- 평온 (emotion_id = 2)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (2, '(닉네임)님, 딱히 무슨 감정이 있는 건 아니지만, 그럴수록 이런 콘텐츠가 필요한 순간일지도 몰라요.', '의미 없는 것들 속에서 가끔 중요한 게 튀어나오기도 해요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (2, '(닉네임)님, 그냥 그런 하루였죠. 아무 생각 없이 클릭해도 괜찮아요.', '가끔은 아무 의도 없이 마주한 것들이, 의외로 오래 남아요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (2, '(닉네임)님, 감정의 여백이 많은 하루, 그 틈을 살짝 채워줄 콘텐츠예요.', '오늘은 아무 기대 없이 읽어도 좋아요. 그런 날도 필요한 법이니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (2, '(닉네임)님, 할 일은 많은데 마음은 비어있을 때, 그냥 스쳐볼 수 있는 콘텐츠가 필요하잖아요.', '모두가 특별할 필요는 없어요. 그냥 그런 오늘도 충분히 의미 있어요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

-- 설렘 (emotion_id = 3)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (3, '(닉네임)님, 설레는 기분이 있다면, 뭔가가 시작될 준비가 된 거예요.', '지금의 감정이 어디로 가게 될지, 조금 더 지켜봐도 좋겠어요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (3, '(닉네임)님의 마음이 조금씩 움직이는 것 같아요. 그 떨림에 어울리는 콘텐츠를 골라봤어요.', '설렘은 가장 강력한 동기부여예요. 지금, 그 에너지를 믿어보세요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (3, '(닉네임)님, 기대하는 일이 있을 때, 마음이 조금 더 섬세해지죠.', '설렘을 오래 붙잡지 않아도 돼요. 잠시 머물러도 충분하니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (3, '(닉네임)님, 어떤 일이든 잘 풀릴 것 같은 예감이 드는 날엔 이런 콘텐츠가 의외로 잘 맞아요.', '좋은 에너지는 늘 다음을 향하고 있어요. 지금도 그중 하나일 거예요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

-- 슬픔 (emotion_id = 4)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (4, '(닉네임)님, 요즘 마음이 자주 무거웠죠. 한 걸음 멈춰, 지금의 나에게 필요한 것들을 함께 살펴봐요.', '계속 괜찮은 척 안 해도 돼요. 마음이 머무를 곳, 여기 있어요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (4, '(닉네임)님, 혼자만 복잡한 게 아니에요. 비슷한 고민을 지나온 이들의 이야기로 당신의 오늘을 밝혀줄게요.', '지금 이 순간, 아무것도 하지 않아도 괜찮아요. 우린 늘 다시 일어설 힘을 가지고 있으니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (4, '생각은 많은데 정리가 안 되죠. (닉네임)님의 머릿속을 환기시켜줄 콘텐츠들을 모았어요', '지금 떠오르지 않아도 괜찮아요. 영감은 가끔, 쉬고 있을 때 더 잘 찾아오거든요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (4, '(닉네임)님, 마음이 무거운 날엔, 다독임보다 방향이 필요한 걸지도 몰라요.', '지금은 가만히 있어도 돼요. 그 자체로도 충분히 잘하고 있는 거니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

-- 짜증 (emotion_id = 5)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (5, '(닉네임)님, 요즘 작은 일에도 쉽게 예민해지죠. 그럴 땐 잠깐 다른 결의 콘텐츠를 보는 것도 괜찮아요.', '짜증을 억누르지 않아도 돼요. 단지, 그 에너지를 흘릴 통로만 있으면 돼요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (5, '(닉네임)님의 답답한 기분, 괜찮아요. 이 콘텐츠들이 잠시 머리를 환기시켜줄 수 있을 거예요.”', '감정은 느끼는 대로 두세요. 똑똑한 선택은 가끔 쉬는 것부터 시작돼요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (5, '(닉네임)님, 짜증이 쌓인 날엔 의미 없는 정보도 거슬리죠. 그래서 가볍고 유연한 콘텐츠들로 준비했어요.', '감정이 무뎌지기 전, 살짝 숨 고르듯 흘려보면 좋아요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (5, '(닉네임)님, 계속해서 끓어오르기 전에, 지금 멈춰서 다른 쪽을 바라보는 것도 하나의 전략이에요.', '감정의 방향을 바꾸기보다, 감정이 흐를 수 있게 해주세요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

-- 분노 (emotion_id = 6)
INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (6, '(닉네임)님, 화가 날 땐 말보다 생각이 많아지죠. 그 감정을 다루기 위한 콘텐츠들을 모았어요.', '감정은 정리하지 않아도 괜찮아요. 다만, 흘릴 구멍은 있어야 하니까요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (6, '(닉네임)님, 억울하고 화가 날 땐, 세상이 너무 불공평하게 느껴지죠. 그럴 때 필요한 건 날카로운 인사이트일지도 몰라요.', '이유 없는 감정은 없어요. 지금 이 감정도 당신의 한 부분이에요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (6, '(닉네임)님, 분노는 무기력이 아니라 에너지예요. 어떻게 쓸지, 지금 여기서 천천히 살펴볼 수 있어요.', '당장 해결은 안 되더라도, 그 감정을 외면하지 않아줘서 고마워요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);

INSERT INTO curation_ment (emotion_id, header_text, footer_text) VALUES
    (6, '(닉네임)님의 감정이 폭발하기 전에 한 번 숨 고를 수 있도록 도와줄 콘텐츠들이에요.', '모든 감정은 지나가요. 오늘의 분노도 결국 당신을 더 단단하게 만들 거예요.')
    ON DUPLICATE KEY UPDATE header_text = VALUES(header_text), footer_text = VALUES(footer_text);



INSERT INTO job (job_id, name) VALUES
    (1, '고등학생'),
    (2, '대학생'),
    (3, '직장인'),
    (4, '자영업자'),
    (5, '프리랜서'),
    (6, '취준생')
    ON DUPLICATE KEY UPDATE name = VALUES(name);


INSERT INTO situation (situation_id, name) VALUES
-- 고등학생 (1~8)
(1, '통학 중'),
(2, '공부 중'),
(3, '식사 중'),
(4, '시험 준비'),
(5, '친구랑'),
(6, '쇼핑 중'),
(7, '휴식 중'),
(8, '자기 전'),

-- 대학생 (9~16)
(9, '과제 중'),
(10, '통학 중'),
(11, '쇼핑 중'),
(12, '알바 중'),
(13, '트렌드 확인'),
(14, '데이트 중'),
(15, '휴식 중'),
(16, '자기 전'),

-- 직장인 (17~24)
(17, '출퇴근'),
(18, '트렌드 확인'),
(19, '업무 중'),
(20, '커리어 고민'),
(21, '쇼핑 중'),
(22, '데이트 중'),
(23, '휴식 중'),
(24, '자기 전'),

-- 자영업자 (25~32)
(25, '출퇴근'),
(26, '업무 준비 중'),
(27, '데이트 중'),
(28, '식사'),
(29, '쇼핑 중'),
(30, '트렌드 확인'),
(31, '휴식 중'),
(32, '자기 전'),

-- 프리랜서 (33~40)
(33, '작업 중'),
(34, '쇼핑 중'),
(35, '트렌드 확인'),
(36, '데이트 중'),
(37, '운동 중'),
(38, '식사'),
(39, '휴식 중'),
(40, '자기 전'),

-- 취준생 (41~48)
(41, '자소서 작성'),
(42, '면접 준비'),
(43, '요리 중'),
(44, '트렌드 확인'),
(45, '쇼핑 중'),
(46, '운동 중'),
(47, '휴식 중'),
(48, '자기 전')
    ON DUPLICATE KEY UPDATE name = VALUES(name);


INSERT INTO situation_job (situation_job_id, situation_id, job_id) VALUES
    (1,1,1),(2,2,1),(3,3,1),(4,4,1),(5,5,1),(6,6,1),(7,7,1),(8,8,1),
    (9,9,2),(10,10,2),(11,11,2),(12,12,2),(13,13,2),(14,14,2),(15,15,2),(16,16,2),
    (17,17,3),(18,18,3),(19,19,3),(20,20,3),(21,21,3),(22,22,3),(23,23,3),(24,24,3),
    (25,25,4),(26,26,4),(27,27,4),(28,28,4),(29,29,4),(30,30,4),(31,31,4),(32,32,4),
    (33,33,5),(34,34,5),(35,35,5),(36,36,5),(37,37,5),(38,38,5),(39,39,5),(40,40,5),
    (41,41,6),(42,42,6),(43,43,6),(44,44,6),(45,45,6),(46,46,6),(47,47,6),(48,48,6)
    ON DUPLICATE KEY UPDATE situation_id = situation_id;

INSERT INTO situation_category (id, situation_id, category_id) VALUES
      (1, 1, 1), (2, 1, 14), (3, 1, 11),
      (4, 2, 3), (5, 2, 5), (6, 2, 10),
      (7, 3, 14), (8, 3, 15),
      (9, 4, 3), (10, 4, 1), (11, 4, 10),
      (12, 5, 14), (13, 5, 12),
      (14, 6, 12), (15, 6, 9),
      (16, 7, 11), (17, 7, 10), (18, 7, 14),
      (19, 8, 11), (20, 8, 10),

      (21, 9, 8), (22, 9, 3), (23, 9, 4),
      (24, 10, 1), (25, 10, 2), (26, 10, 14),
      (27, 11, 12), (28, 11, 9),
      (29, 12, 5), (30, 12, 7),
      (31, 13, 12), (32, 13, 2), (33, 13, 13),
      (34, 14, 15), (35, 14, 14),
      (36, 15, 11), (37, 15, 10),
      (38, 16, 11), (39, 16, 14),

      (40, 17, 2), (41, 17, 14), (42, 17, 12),
      (43, 18, 12), (44, 18, 7), (45, 18, 4),
      (46, 19, 8), (47, 19, 7),
      (48, 20, 6), (49, 20, 5), (50, 20, 10),
      (51, 21, 12), (52, 21, 9),
      (53, 22, 15), (54, 22, 14),
      (55, 23, 10), (56, 23, 11),
      (57, 24, 11), (58, 24, 14),

      (59, 25, 2), (60, 25, 12),
      (61, 26, 8), (62, 26, 7),
      (63, 27, 15), (64, 27, 14),
      (65, 28, 15), (66, 28, 9),
      (67, 29, 12), (68, 29, 9),
      (69, 30, 12), (70, 30, 2), (71, 30, 13),
      (72, 31, 11), (73, 31, 10),
      (74, 32, 11), (75, 32, 14),

      (76, 33, 8), (77, 33, 13),
      (78, 34, 12), (79, 34, 9),
      (80, 35, 12), (81, 35, 13), (82, 35, 4),
      (83, 36, 15), (84, 36, 14),
      (85, 37, 9), (86, 37, 14),
      (87, 38, 15),
      (88, 39, 11), (89, 39, 10),
      (90, 40, 14), (91, 40, 10),

      (92, 41, 6), (93, 41, 5),
      (94, 42, 6), (95, 42, 10),
      (96, 43, 9), (97, 43, 14),
      (98, 44, 12), (99, 44, 2), (100, 44, 7),
      (101, 45, 12), (102, 45, 9),
      (103, 46, 14), (104, 46, 9),
      (105, 47, 11), (106, 47, 10),
      (107, 48, 10), (108, 48, 14)
    ON DUPLICATE KEY UPDATE situation_id = situation_id;

