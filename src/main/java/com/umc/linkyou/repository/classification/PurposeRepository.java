package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Purposes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 사용 목적 마스터(카탈로그) 레포지토리
// 값은 domain.enums.Purpose 기준으로 고정되어 있고 V9 마이그레이션에서 이미 시딩되어 있다.
// 따라서 요청 시점에 새로 만들 필요가 없고, 조회만 한다.
public interface PurposeRepository extends JpaRepository<Purposes, Long> {
    Optional<Purposes> findByName(String name);

    List<Purposes> findAllByNameIn(Collection<String> names);
}
