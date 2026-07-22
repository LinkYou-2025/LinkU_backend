package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alarms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "alarm_type", length = 100, nullable = false)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Builder(access = AccessLevel.PRIVATE)
    private Alarm(
            AlarmType alarmType,
            Long targetId,
            String body
    ) {
        this.alarmType = alarmType;
        this.title = alarmType.getTitle();
        this.body = body;
        this.targetId = targetId;
    }

    @OneToMany(mappedBy = "alarm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAlarm> userAlarms = new ArrayList<>();

    public static Alarm create(AlarmType alarmType, Long targetId) {
        return create(alarmType, targetId, alarmType.getBody());
    }

    public static Alarm create(AlarmType alarmType, Long targetId, String renderedBody) {
        return Alarm.builder()
                .alarmType(alarmType)
                .targetId(targetId)
                .body(renderedBody)
                .build();
    }

    public static Alarm createWithBody(AlarmType alarmType, Long targetId, String body) {
        return create(alarmType, targetId, body);
    }

    public void updateTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
