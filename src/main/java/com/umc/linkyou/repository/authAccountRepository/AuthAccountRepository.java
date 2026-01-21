package com.umc.linkyou.repository.authAccountRepository;

import com.umc.linkyou.domain.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long>, AuthAccountRepositoryCustom {
}
