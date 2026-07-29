package com.umc.linkyou.repository;

import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {
    @Query("""
            select ua
            from UserAlarm ua
            join fetch ua.alarm a
            where ua.user.id = :userId
              and ua.id < :cursor
            order by ua.id desc
            """)
    List<UserAlarm> findAlarmListByCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select ua
            from UserAlarm ua
            join fetch ua.alarm a
            where ua.user.id = :userId
              and ua.id < :cursor
              and a.alarmType in :alarmTypes
            order by ua.id desc
            """)
    List<UserAlarm> findAlarmListByCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("alarmTypes") List<AlarmType> alarmTypes,
            Pageable pageable
    );

    UserAlarm findByUserAndAlarm(Users user, Alarm alarm);

    boolean existsByUser_IdAndIsReadFalseAndCreatedAtAfter(Long userId, LocalDateTime after);

    void deleteByCreatedAtBefore(LocalDateTime threshold);

}
