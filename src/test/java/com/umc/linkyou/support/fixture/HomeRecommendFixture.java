package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.SituationJob;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class HomeRecommendFixture {
    public static final long USER_ID = 48L;
    public static final long SITUATION_ID = 3L;
    public static final long EMOTION_ID = 5L;
    public static final long JOB_ID = 1L;
    public static final long CATEGORY_ID = 16L;

    private HomeRecommendFixture() {}

    public static Users user() {
        return Users.builder().id(USER_ID).job(Job.builder().id(JOB_ID).name("직장인").build()).build();
    }

    public static Users userWithoutJob() {
        return Users.builder().id(USER_ID).build();
    }

    public static Emotion emotion() {
        return Emotion.builder().emotionId(EMOTION_ID).name("행복").build();
    }

    public static Situation situation() {
        return Situation.builder().id(SITUATION_ID).name("공부").build();
    }

    public static SituationJob situationJob() {
        return SituationJob.builder().situation(situation()).job(user().getJob()).build();
    }

    public static RankedUsersLinku candidate(long id, int score) {
        return new RankedUsersLinku(id, id + 100L, CATEGORY_ID, "https://example.com/" + id,
                null, EMOTION_ID, "추천 링크 " + id, "웹사이트", null, null, false, null, score);
    }

    public static LinkuFolder folder(long userLinkuId, long mappingId, String name) {
        return LinkuFolder.builder().linkuFolderId(mappingId)
                .usersLinku(UsersLinku.builder().userLinkuId(userLinkuId).build())
                .folder(Folder.builder().folderId(mappingId).folderName(name).build()).build();
    }

    public static String encodedJson(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
