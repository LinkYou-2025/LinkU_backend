package com.umc.linkyou.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.Alarm;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
