package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Interests;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 관심사 마스터(카탈로그) 레포지토리. 값은 domain.enums.Interest 기준으로 고정, V9에서 시딩됨.
public interface InterestRepository extends JpaRepository<Interests, Long> {
    Optional<Interests> findByName(String name);

    List<Interests> findAllByNameIn(Collection<String> names);
}
