package com.umc.linkyou.repository;

import com.umc.linkyou.domain.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
