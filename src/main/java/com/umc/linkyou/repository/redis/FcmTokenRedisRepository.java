package com.umc.linkyou.repository.redis;

import org.springframework.data.repository.CrudRepository;

import com.umc.linkyou.domain.redis.UserFcmTokenCache;

public interface FcmTokenRedisRepository extends CrudRepository<UserFcmTokenCache, Long> {}
