package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_alarm")
public class UserAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
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
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
