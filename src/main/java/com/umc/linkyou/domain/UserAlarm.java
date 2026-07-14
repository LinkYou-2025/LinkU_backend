package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_alarms",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_alarm_user_alarm",
                columnNames = {"user_id", "alarm_id"}
        ),
        // 미읽음 알림 존재 여부 조회용 인덱스
        indexes = @Index(
                name = "idx_user_alarms_unread",
                columnList = "user_id, is_read, created_at"
        )
)
public class UserAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id", nullable = false)
    private Alarm alarm;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserAlarm(
            Users user,
            Alarm alarm,
            LocalDateTime deliveredAt
    ) {
        this.user = user;
        this.alarm = alarm;
        this.isRead = false;
        this.deliveredAt = deliveredAt;
    }


    public static UserAlarm create(
            Users user,
            Alarm alarm
    ) {
        return UserAlarm.builder()
                .user(user)
                .alarm(alarm)
                .deliveredAt(LocalDateTime.now())
                .build();
    }

    public void markAsRead() {
        if (this.isRead) return;
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
