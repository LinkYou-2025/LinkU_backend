package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarm")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curation_alarm_id")
    private Long id;

    // 읽음 여부
    @Column(nullable = false)
    private Boolean isConfirmed = false;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String body;


    @Column(length = 100, nullable = false)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;
}
