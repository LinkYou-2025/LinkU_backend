package com.umc.linkyou.repository.redis;

import com.umc.linkyou.domain.redis.PasswordResetCache;
import org.springframework.data.repository.CrudRepository;

public interface PasswordResetRedisRepository extends CrudRepository<PasswordResetCache, String> {
}
