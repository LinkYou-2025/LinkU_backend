package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Interests;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 관심사 마스터(카탈로그) 레포지토리
public interface InterestRepository extends JpaRepository<Interests, Long> {
    Optional<Interests> findByName(String name);

    List<Interests> findAllByNameIn(Collection<String> names);
}
