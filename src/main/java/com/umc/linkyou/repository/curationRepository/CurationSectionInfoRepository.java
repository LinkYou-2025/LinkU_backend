package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.CurationSectionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurationSectionInfoRepository extends JpaRepository<CurationSectionInfo, Long> {

    List<CurationSectionInfo> findAllByMonthOrderBySectionNumberAsc(String month);
}