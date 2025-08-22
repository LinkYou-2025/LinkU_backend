# INSERT INTO category (category_id, color_code, name) VALUES (1, '#000000', '어학')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (2, '#000000', '뉴스')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (3, '#000000', '공부법')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (4, '#000000', 'IT·개발')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (5, '#000000', '자기계발')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (6, '#000000', '취업·이직')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (7, '#000000', '비즈니스 인사이트')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (8, '#000000', '생산성·툴')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (9, '#000000', '라이프스타일')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (10, '#000000', '심리·자기이해')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (11, '#000000', '에세이·칼럼')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (12, '#000000', '트렌드')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (13, '#000000', '디자인·예술')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (14, '#000000', '영상·뮤직')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (15, '#000000', '맛집·여행')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
# INSERT INTO category (category_id, color_code, name) VALUES (16, '#000000', '기타')
#     ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (1, 1, '어학', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (2, 2, '뉴스', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (3, 3, '공부법', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (4, 4, 'IT·개발', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (5, 5, '자기계발', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (6, 6, '취업·이직', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (7, 7, '비즈니스 인사이트', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (8, 8, '생산성·툴', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (9, 9, '라이프스타일', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (10, 10, '심리·자기이해', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (11, 11, '에세이·칼럼', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (12, 12, '트렌드', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (13, 13, '디자인·예술', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (14, 14, '영상·뮤직', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (15, 15, '맛집·여행', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);

INSERT INTO folder (folder_id, category_id, folder_name, parent_folder_id, created_at, updated_at)
VALUES (16, 16, '기타', NULL, NOW(), NOW())
    ON DUPLICATE KEY UPDATE folder_name = VALUES(folder_name), category_id = VALUES(category_id);


INSERT INTO emotion (emotion_id, name) VALUES (1, '즐거움') ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name) VALUES (2, '평온') ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name) VALUES (3, '설렘') ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name) VALUES (4, '슬픔') ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name) VALUES (5, '짜증') ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO emotion (emotion_id, name) VALUES (6, '분노') ON DUPLICATE KEY UPDATE name = VALUES(name);



INSERT INTO domain (domain_id, name, domain_tail, image_url) VALUES
(1, 'invalid', 'invalid', NULL),
(2, 'blog.naver', 'blog.naver.com', NULL),
(3, 'cafe.naver', 'cafe.naver.com', NULL),
(4, 'kin.naver', 'kin.naver.com', NULL),
(5, 'shopping.naver', 'shopping.naver.com', NULL),
(6, 'github', 'github.com', NULL),
(7, 'linkedin', 'linkedin.com', NULL),
(8, 'tistory', 'tistory.com', NULL),
(9, 'google', 'google.com', NULL),
(10, 'nytimes', 'nytimes.com', NULL),
(11, 'brunch', 'brunch.co.kr', NULL),
(12, 'velog', 'velog.io', NULL),
(13, 'daum', 'daum.net', NULL),
(14, 'jobkorea', 'jobkorea.co.kr', NULL),
(15, 'wanted', 'wanted.co.kr', NULL),
(16, 'musinsa', 'musinsa.com', NULL),
(17, '11st', '11st.co.kr', NULL),
(18, 'instagram', 'instagram.com', NULL),
(19, 'twitter', 'twitter.com', NULL),
(20, 'facebook', 'facebook.com', NULL),
(21, 'naver', 'naver.com', NULL)
    ON DUPLICATE KEY UPDATE
                         name = VALUES(name),
                         domain_tail = VALUES(domain_tail),
                         image_url = VALUES(image_url);
