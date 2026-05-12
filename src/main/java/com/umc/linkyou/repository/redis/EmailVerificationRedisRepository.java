package com.umc.linkyou.repository.redis;

import com.umc.linkyou.domain.redis.EmailVerificationCache;
import org.springframework.data.repository.CrudRepository;

public interface EmailVerificationRedisRepository extends CrudRepository<EmailVerificationCache, String> {
}
