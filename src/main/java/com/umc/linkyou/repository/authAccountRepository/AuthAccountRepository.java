package com.umc.linkyou.repository.authAccountRepository;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long>, AuthAccountRepositoryCustom {
    boolean existsByUserIdAndProvider(Long userId, Provider provider);

    boolean existsByProviderAndExternalId(Provider provider, String externalId);
    Optional<AuthAccount> findByUserIdAndProvider(Long userId, Provider provider);
    boolean existsByEmail(String email);
}
