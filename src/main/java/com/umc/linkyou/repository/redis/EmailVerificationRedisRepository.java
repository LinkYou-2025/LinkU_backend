package com.umc.linkyou.repository.redis;

import org.springframework.data.repository.CrudRepository;

import com.umc.linkyou.domain.redis.EmailVerificationCache;

public interface EmailVerificationRedisRepository
        extends CrudRepository<EmailVerificationCache, String> {}
