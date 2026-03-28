package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_alarm")
public class UserAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_alarm_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private AlarmType type;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read")
    private boolean isRead;

    @Builder(access = AccessLevel.PRIVATE)
    private UserAlarm(
            Long userId,
            AlarmType type,
            String title,
            String body,
            Long targetId
    ) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetId = targetId;
        this.isRead = false;
    }


    public static UserAlarm create(
            Long userId,
            AlarmType type,
            Long targetId
    ) {
        return UserAlarm.builder()
                .userId(userId)
                .type(type)
                .title(type.getTitle())
                .body(type.getBody())
                .targetId(targetId)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
