package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_alarm")
public class NoticeAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_alarm_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private AlarmType type;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;

    @Lob
    @Column(name = "content")
    private String content;

    @Builder(access = AccessLevel.PRIVATE)
    private NoticeAlarm(
            AlarmType type,
            String title,
            String body,
            String content
    ) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.content = content;
    }

    public static NoticeAlarm create(
            AlarmType type,
            String title,
            String body,
            String content
    ) {
        return NoticeAlarm.builder()
                .type(type)
                .title(title)
                .body(body)
                .content(content)
                .build();
    }
}
