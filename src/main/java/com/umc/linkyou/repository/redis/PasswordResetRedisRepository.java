package com.umc.linkyou.repository.redis;

import org.springframework.data.repository.CrudRepository;

import com.umc.linkyou.domain.redis.PasswordResetCache;

public interface PasswordResetRedisRepository extends CrudRepository<PasswordResetCache, String> {}
