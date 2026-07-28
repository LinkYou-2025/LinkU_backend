package com.umc.linkyou.repository.dto;

import com.umc.linkyou.domain.mapping.UsersLinku;

/**
 * 홈화면 추천 seek(keyset) 페이징 결과 한 건. scoreBucket은 정렬에 쓰인 score를 정수 구간으로
 * 양자화한 값으로(HomeRecommendScoreService#scoreBucketExpression), 다음 페이지 요청 시 커서의
 * 탐색 키 (scoreBucket, usersLinku.getUserLinkuId())로 그대로 쓰인다.
 */
public record RankedUsersLinku(UsersLinku usersLinku, int scoreBucket) {}
