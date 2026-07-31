package com.umc.linkyou.repository.dto;

import com.umc.linkyou.domain.mapping.UsersLinku;

/** seek 페이징 후보 1건. scoreBucket은 다음 커서의 탐색 키로 쓰인다 */
public record RankedUsersLinku(UsersLinku usersLinku, int scoreBucket) {}
