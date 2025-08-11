package com.umc.linkyou.repository;

import com.umc.linkyou.domain.UserRefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRefreshTokenRepository extends CrudRepository<UserRefreshToken, String> {
    // 단일 세션 모델
    Optional<UserRefreshToken> findByUserId(Long userId);

    // 여러 기기/세션 허용이면 아래처럼 쓰는 편이 안전
    //Iterable<UserRefreshToken> findAllByUserId(Long userId);
}