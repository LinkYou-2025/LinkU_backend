package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkuRepository extends JpaRepository<Linku, Long> {
    Optional<Linku> findByLinku(String normalizedLink);
}
