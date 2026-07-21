package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.LinkuSearchHistory;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;

import java.util.List;

public final class LinkuFixture {

    public static final Long USER_ID = 1L;
    public static final Long CATEGORY_ID = 16L;
    public static final Long EMOTION_ID = 1L;
    public static final Long SITUATION_ID = 1L;
    public static final String TEST_URL = "https://example.com/article";
    public static final String CRAWLED_IMAGE_URL = "https://crawled.com/image.jpg";
    public static final String S3_IMAGE_URL = "https://s3.amazonaws.com/bucket/user-upload.jpg";

    private LinkuFixture() {}

    public static Users user() {
        return Users.builder()
                .id(USER_ID)
                .nickName("testUser")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    public static Category category() {
        return Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("기술")
                .build();
    }

    public static Domain domain() {
        return Domain.builder()
                .domainId(1L)
                .name("example")
                .domainTail("example.com")
                .build();
    }

    public static Emotion emotion() {
        return Emotion.builder()
                .emotionId(EMOTION_ID)
                .name("기대")
                .build();
    }

    public static Situation situation() {
        return Situation.builder()
                .id(SITUATION_ID)
                .name("공부")
                .build();
    }

    public static Folder folder() {
        return Folder.builder()
                .folderId(10L)
                .folderName("기술")
                .category(category())
                .build();
    }

    public static Linku linku(String imgUrl) {
        return Linku.builder()
                .linkuId(100L)
                .linkuUrl(TEST_URL)
                .title("테스트 제목")
                .category(category())
                .domain(domain())
                .imgUrl(imgUrl)
                .build();
    }

    public static Linku linkuWithoutImage() {
        return linku(null);
    }

    public static LinkuSearchResponseDTO.LinkuSearchItemDTO searchItem(Long userLinkuId, String title) {
        return new LinkuSearchResponseDTO.LinkuSearchItemDTO(
                userLinkuId, userLinkuId, title, null, List.of(), "https://img.example.com/icon.png", "example");
    }

    public static LinkuQuickSearchResponseDTO quickSearchItem(String title, Long userLinkuId) {
        return new LinkuQuickSearchResponseDTO(title, "https://img.example.com/icon.png", userLinkuId);
    }

    public static LinkuSearchHistory searchHistory(Long id, Long userId, String keyword) {
        return LinkuSearchHistory.builder()
                .id(id)
                .userId(userId)
                .keyword(keyword)
                .build();
    }

    public static AiArticle aiArticle(Linku linku, String summary) {
        return AiArticle.builder()
                .id(1L)
                .linku(linku)
                .title("테스트 AI 제목")
                .summary(summary)
                .build();
    }
}
