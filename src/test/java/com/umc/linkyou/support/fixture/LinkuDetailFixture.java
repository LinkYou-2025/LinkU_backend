package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.mapping.UsersLinku;
import jakarta.persistence.EntityManager;

public final class LinkuDetailFixture {
    private LinkuDetailFixture() {}

    public record Saved(Long userId, Long userLinkuId, Long linkuId) {}

    public static Saved persist(EntityManager em, boolean hasArticle, boolean hasSituation) {
        Users user = Users.builder().nickName("detail-owner").password("encoded-password")
                .role(Role.USER).build();
        em.persist(user);
        Fcolor color = Fcolor.builder().colorName("blue").colorCode1("#001")
                .colorCode2("#002").colorCode3("#003").colorCode4("#004").build();
        em.persist(color);
        Category category = Category.builder().categoryName("기술").fcolor(color).build();
        em.persist(category);
        Domain domain = Domain.builder().name("example").domainTail("example.com").build();
        em.persist(domain);
        Emotion emotion = Emotion.builder().name("기대").build();
        em.persist(emotion);
        Situation situation = Situation.builder().name("공부").build();
        em.persist(situation);
        Linku linku = Linku.builder().linkuUrl("https://example.com/detail").title("원문 제목")
                .category(category).domain(domain).emotion(emotion).situation(situation).build();
        em.persist(linku);
        UsersLinku ul = UsersLinku.builder().user(user).linku(linku).emotion(emotion)
                .situation(hasSituation ? situation : null).aiExist(hasArticle).title("개인 제목").build();
        em.persist(ul);
        if (hasArticle) {
            em.persist(AiArticle.builder().linku(linku).summary("테스트 요약").build());
            for (String name : new String[] {"Java", "Spring"}) {
                Keyword keyword = Keyword.builder().name(name).build();
                em.persist(keyword);
                em.persist(LinkuKeyword.builder().linku(linku).keyword(keyword).build());
            }
        }
        for (String name : new String[] {"이전 폴더", "최신 폴더"}) {
            Folder folder = Folder.builder().folderName(name).category(category).build();
            em.persist(folder);
            em.persist(LinkuFolder.builder().usersLinku(ul).folder(folder).build());
        }
        em.flush();
        Saved saved = new Saved(user.getId(), ul.getUserLinkuId(), linku.getLinkuId());
        em.clear();
        return saved;
    }
}
