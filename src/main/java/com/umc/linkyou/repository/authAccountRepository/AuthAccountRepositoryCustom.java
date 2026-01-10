package com.umc.linkyou.repository.authAccountRepository;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.enums.Provider;

import java.util.Optional;

public interface AuthAccountRepositoryCustom {

    Optional<AuthAccount> findByProviderAndExternalId(Provider provider, String externalId);
}