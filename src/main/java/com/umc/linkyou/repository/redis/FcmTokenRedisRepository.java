package com.umc.linkyou.repository.redis;

import com.umc.linkyou.domain.redis.UserFcmTokenCache;
import org.springframework.data.repository.CrudRepository;

public interface FcmTokenRedisRepository extends CrudRepository<UserFcmTokenCache, Long> {
}
