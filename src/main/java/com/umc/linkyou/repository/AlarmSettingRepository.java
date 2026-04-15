package com.umc.linkyou.repository;

import com.umc.linkyou.domain.AlarmSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlarmSettingRepository extends JpaRepository<AlarmSetting,Long> {
    Optional<AlarmSetting> findByUserId(Long userId);
}
