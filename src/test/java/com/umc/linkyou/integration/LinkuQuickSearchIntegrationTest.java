package com.umc.linkyou.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.support.security.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestExternalConfig.class})
@Transactional
@DisplayName("링크 검색·자동완성 통합테스트")
class LinkuQuickSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LinkuRepository linkuRepository;

    @Autowired
    private UsersLinkuRepository usersLinkuRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FcolorRepository fcolorRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private SituationRepository situationRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private LinkuKeywordRepository linkuKeywordRepository;

    private Domain domain;
    private Category category;
    private Emotion emotion;
    private Situation situation;

    @BeforeEach
    void setUp() {
        domain = domainRepository.save(Domain.builder()
                .domainTail("youtube.com")
                .name("유튜브")
                .imageUrl("https://image.com/youtube.png")
                .build());

        Fcolor fcolor = fcolorRepository.save(Fcolor.builder()
                .colorName("BLUE")
                .colorCode1("#E3F2FD")
                .colorCode2("#90CAF9")
                .colorCode3("#42A5F5")
                .colorCode4("#1E88E5")
                .build());

        category = categoryRepository.save(Category.builder()
                .categoryName("개발")
                .fcolor(fcolor)
                .build());

        emotion = emotionRepository.save(Emotion.builder()
                .name("기쁨")
                .build());

        situation = situationRepository.save(Situation.builder()
                .name("일상")
                .build());
    }

    // ===== 링크 검색 (커서 페이징) =====

    @Test
    @DisplayName("검색 - 제목 중간 단어로 검색해도 링크가 반환된다")
    void search_byWordInMiddleOfTitle_returnsLink() throws Exception {
        Users user = createUser("search_user_1");
        Linku linku = createLinku("스프링 부트 강의 모음", "https://youtube.com/watch?v=1");
        saveToUser(user, linku, null);

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "부트")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LINKU2005"))
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].title").value("스프링 부트 강의 모음"))
                .andExpect(jsonPath("$.result.items[0].domainName").value("유튜브"))
                .andExpect(jsonPath("$.result.items[0].domainImageUrl").value("https://image.com/youtube.png"))
                .andExpect(jsonPath("$.result.items[0].tags.length()").value(0))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("검색 - 사용자가 지정한 제목(users_linku.title)으로 검색해도 링크가 반환된다")
    void search_byUserCustomTitle_returnsLink() throws Exception {
        Users user = createUser("search_user_2");
        Linku linku = createLinku("원본 크롤링 제목", "https://youtube.com/watch?v=2");
        saveToUser(user, linku, "내가 바꾼 제목");

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "바꾼")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].title").value("내가 바꾼 제목"));
    }

    @Test
    @DisplayName("검색 - 제목엔 없고 태그에만 있는 단어로 검색해도 링크가 반환된다")
    void search_byTagOnly_returnsLink() throws Exception {
        Users user = createUser("search_user_3");
        Linku linku = createLinku("자바 유용한 팁", "https://youtube.com/watch?v=3");
        saveToUser(user, linku, null);
        attachTag(linku, "스프링");

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].title").value("자바 유용한 팁"))
                .andExpect(jsonPath("$.result.items[0].tags[0]").value("스프링"));
    }

    @Test
    @DisplayName("검색 - 영문 대소문자를 구분하지 않는다")
    void search_isCaseInsensitive() throws Exception {
        Users user = createUser("search_user_4");
        Linku linku = createLinku("YouTube 알고리즘 분석", "https://youtube.com/watch?v=4");
        saveToUser(user, linku, null);

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "youtube")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].title").value("YouTube 알고리즘 분석"));
    }

    @Test
    @DisplayName("검색 - 다른 사용자가 저장한 링크는 검색되지 않는다")
    void search_doesNotReturnOtherUsersLinks() throws Exception {
        Users owner = createUser("search_user_5");
        Users other = createUser("search_user_6");
        Linku linku = createLinku("스프링 부트 강의", "https://youtube.com/watch?v=5");
        saveToUser(owner, linku, null);

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(0))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("검색 - 최신 저장 순으로 반환된다")
    void search_returnsNewestFirst() throws Exception {
        Users user = createUser("search_user_7");
        Linku first = createLinku("스프링 강의 하나", "https://youtube.com/watch?v=6");
        saveToUser(user, first, null);
        Linku second = createLinku("스프링 강의 둘", "https://youtube.com/watch?v=7");
        saveToUser(user, second, null);

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].title").value("스프링 강의 둘"))
                .andExpect(jsonPath("$.result.items[1].title").value("스프링 강의 하나"));
    }

    @Test
    @DisplayName("검색 - 커서 페이징: 첫 페이지 후 nextCursor로 다음 페이지를 이어서 조회한다")
    void search_cursorPaging() throws Exception {
        Users user = createUser("search_user_8");
        for (int i = 1; i <= 15; i++) {
            Linku linku = createLinku("스프링 강의 " + i, "https://youtube.com/watch?v=paging" + i);
            saveToUser(user, linku, null);
        }

        MvcResult firstPage = mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "스프링")
                        .param("size", "10")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(10))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.nextCursor").isNumber())
                .andReturn();

        JsonNode root = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        long nextCursor = root.path("result").path("nextCursor").asLong();

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "스프링")
                        .param("cursor", String.valueOf(nextCursor))
                        .param("size", "10")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(5))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("검색 - 검색어가 공백뿐이면 400을 반환한다")
    void search_blankKeyword_badRequest() throws Exception {
        Users user = createUser("search_user_9");

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", "   ")
                        .with(authentication(authFor(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINKU4003"));
    }

    @Test
    @DisplayName("검색 - 검색어가 1글자면 400을 반환한다")
    void search_singleCharKeyword_badRequest() throws Exception {
        Users user = createUser("search_user_10");

        mockMvc.perform(get("/api/v1/linku/search")
                        .param("keyword", " 스 ")
                        .with(authentication(authFor(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINKU4005"));
    }

    // ===== 검색어 자동완성 =====

    @Test
    @DisplayName("자동완성 - 저장한 링크의 제목에서 후보를 반환한다")
    void autocomplete_returnsTitleCandidates() throws Exception {
        Users user = createUser("search_user_11");
        Linku linku = createLinku("스프링 부트 강의", "https://youtube.com/watch?v=8");
        saveToUser(user, linku, null);

        mockMvc.perform(get("/api/v1/linku/search/quick")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LINKU2008"))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].title").value("스프링 부트 강의"))
                .andExpect(jsonPath("$.result[0].domainImageUrl").value("https://image.com/youtube.png"))
                .andExpect(jsonPath("$.result[0].userLinkuId").isNumber());
    }

    @Test
    @DisplayName("자동완성 - 태그만 일치하는 링크는 후보에 나오지 않는다")
    void autocomplete_excludesTagOnlyMatches() throws Exception {
        Users user = createUser("search_user_12");
        Linku linku = createLinku("자바 유용한 팁", "https://youtube.com/watch?v=9");
        saveToUser(user, linku, null);
        attachTag(linku, "스프링");

        mockMvc.perform(get("/api/v1/linku/search/quick")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    @DisplayName("자동완성 - 후보는 최대 3개까지만 반환된다")
    void autocomplete_returnsAtMostThree() throws Exception {
        Users user = createUser("search_user_13");
        for (int i = 1; i <= 5; i++) {
            Linku linku = createLinku("스프링 강의 " + i, "https://youtube.com/watch?v=quick" + i);
            saveToUser(user, linku, null);
        }

        mockMvc.perform(get("/api/v1/linku/search/quick")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(3));
    }

    @Test
    @DisplayName("자동완성 - 사용자가 지정한 제목으로도 검색된다")
    void autocomplete_byCustomTitle_returnsLink() throws Exception {
        Users user = createUser("search_user_15");
        Linku linku = createLinku("원본 크롤링 제목", "https://youtube.com/watch?v=custom");
        saveToUser(user, linku, "내가 바꾼 스프링 강의");

        mockMvc.perform(get("/api/v1/linku/search/quick")
                        .param("keyword", "스프링")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].title").value("내가 바꾼 스프링 강의"));
    }

    @Test
    @DisplayName("자동완성 - 검색어가 1글자면 400을 반환한다")
    void autocomplete_singleCharKeyword_badRequest() throws Exception {
        Users user = createUser("search_user_14");

        mockMvc.perform(get("/api/v1/linku/search/quick")
                        .param("keyword", "스")
                        .with(authentication(authFor(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINKU4005"));
    }

    // ===== 헬퍼 =====

    private Users createUser(String nickName) {
        return userRepository.save(Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build());
    }

    private Linku createLinku(String title, String url) {
        return linkuRepository.save(Linku.builder()
                .title(title)
                .linkuUrl(url)
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .situation(situation)
                .build());
    }

    private void saveToUser(Users user, Linku linku, String customTitle) {
        usersLinkuRepository.save(UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .title(customTitle)
                .build());
    }

    private void attachTag(Linku linku, String tagName) {
        Keyword tag = keywordRepository.findByName(tagName)
                .orElseGet(() -> keywordRepository.save(Keyword.builder().name(tagName).build()));
        linkuKeywordRepository.save(LinkuKeyword.builder()
                .linku(linku)
                .keyword(tag)
                .build());
    }

    private Authentication authFor(Users user) {
        CustomUserDetails principal = new CustomUserDetails(user, "kakao");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}
