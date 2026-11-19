package com.umc.linkyou.repository.authAccountRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.enums.Provider;

public interface AuthAccountRepository
        extends JpaRepository<AuthAccount, Long>, AuthAccountRepositoryCustom {
    boolean existsByUserIdAndProvider(Long userId, Provider provider);

    boolean existsByProviderAndExternalId(Provider provider, String externalId);

    Optional<AuthAccount> findByUserIdAndProvider(Long userId, Provider provider);

    boolean existsByEmail(String email);

    void deleteAllByUser_Id(Long userId);

    List<AuthAccount> findByEmail(String email);
}
