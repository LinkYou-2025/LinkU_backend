package com.umc.linkyou.repository.authAccountRepository;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long>, AuthAccountRepositoryCustom {
    boolean existsByUserIdAndProvider(Long userId, Provider provider);
}
