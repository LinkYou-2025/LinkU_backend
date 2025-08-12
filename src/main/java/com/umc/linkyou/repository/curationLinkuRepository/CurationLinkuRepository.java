package com.umc.linkyou.repository.curationLinkuRepository;

import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CurationLinkuRepository extends JpaRepository<CurationLinku, Long> {

    List<CurationLinku> findByCuration_CurationIdAndType(Long curationId, CurationLinkuType type);

    @Modifying
    @Query("delete from CurationLinku cl where cl.curation.curationId = :curationId and cl.type = :type")
    void deleteAllByCurationIdAndType(Long curationId, CurationLinkuType type);
}