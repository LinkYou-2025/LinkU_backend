package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurationLinkuRepository extends JpaRepository<CurationLinku, Long> {

    @Query("""
            SELECT cl FROM CurationLinku cl
            WHERE cl.curation.curationId = :curationId AND cl.type = :type
            """)
    List<CurationLinku> findByCurationIdAndType(
            @Param("curationId") Long curationId,
            @Param("type") CurationLinkuType type);

    @Query("""
            SELECT cl FROM CurationLinku cl
            JOIN FETCH cl.usersLinku ul
            JOIN FETCH ul.linku l
            LEFT JOIN FETCH l.domain
            WHERE cl.curation.curationId = :curationId AND cl.type = :type
            """)
    List<CurationLinku> findWithDomainByCurationIdAndType(
            @Param("curationId") Long curationId,
            @Param("type") CurationLinkuType type);

    @Modifying
    @Query("""
            DELETE FROM CurationLinku cl
            WHERE cl.curation.curationId = :curationId AND cl.type = :type
            """)
    void deleteAllByCurationIdAndType(
            @Param("curationId") Long curationId,
            @Param("type") CurationLinkuType type);

    @Query("""
            SELECT cl FROM CurationLinku cl
            WHERE cl.usersLinku.userLinkuId = :userLinkuId
            """)
    List<CurationLinku> findByUsersLinkuId(@Param("userLinkuId") Long userLinkuId);

}
