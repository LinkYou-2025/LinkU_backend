package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_notice_alarm",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "notice_alarm_id"})
        }
)
public class UserNoticeAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notice_alarm_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notice_alarm_id", nullable = false)
    private Long noticeAlarmId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserNoticeAlarm(
            Long userId,
            Long noticeAlarmId
    ) {
        this.userId = userId;
        this.noticeAlarmId = noticeAlarmId;
        this.readAt = LocalDateTime.now();
    }

    public static UserNoticeAlarm read(Long userId, Long noticeAlarmId) {
        return UserNoticeAlarm.builder()
                .userId(userId)
                .noticeAlarmId(noticeAlarmId)
                .build();
    }
}
