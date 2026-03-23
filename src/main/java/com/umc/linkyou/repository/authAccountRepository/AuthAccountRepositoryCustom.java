package com.umc.linkyou.repository.authAccountRepository;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;

import java.util.Optional;

public interface AuthAccountRepositoryCustom {

    Optional<AuthAccount> findByProviderAndExternalId(Provider provider, String externalId);
    // UserId만 반환하는 심플한 메서드
    Optional<Long> findUserIdByEmailAndProvider(String email, Provider provider);
    Optional<Users> findUserByEmail(String email);
    // 또는 User 엔티티만 (email은 input으로 충분)
    Optional<Users> findUserByEmailAndProvider(String email, Provider provider);
    Optional<Users> findUserByEmailExcludingProvider(String email, Provider provider);
    boolean existsByUserIdAndProviderNot(Long userId, Provider provider); //특정 유저에게 카카오가 아닌 다른 로그인 수단이 있는지
    Optional<String> findEmailByUserIdAndProvider(Long userId, Provider provider);
}
