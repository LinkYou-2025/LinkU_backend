package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alarm")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Column(length = 100, nullable = false)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Builder(access = AccessLevel.PRIVATE)
    private Alarm(
            AlarmType alarmType,
            Long targetId
    ) {
        this.alarmType = alarmType;
        this.title = alarmType.getTitle();
        this.body = alarmType.getBody();
        this.targetId = targetId;
    }

    @OneToMany(mappedBy = "alarm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAlarm> userAlarms = new ArrayList<>();

    public static Alarm create(AlarmType alarmType, Long targetId) {
        return Alarm.builder()
                .alarmType(alarmType)
                .targetId(targetId)
                .build();
    }

    public void updateTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
