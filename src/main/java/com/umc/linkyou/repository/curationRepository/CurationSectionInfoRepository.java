package com.umc.linkyou.repository.curationRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.linkyou.domain.CurationSectionInfo;

public interface CurationSectionInfoRepository extends JpaRepository<CurationSectionInfo, Long> {

    @Query(
            """
            SELECT cs FROM CurationSectionInfo cs
            WHERE cs.month = :month
            ORDER BY cs.sectionNumber ASC
            """)
    List<CurationSectionInfo> findAllByMonth(@Param("month") String month);

    Optional<CurationSectionInfo> findByMonthAndSectionNumber(String month, int sectionNumber);
}
