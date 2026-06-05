package com.umc.linkyou.domain;


import com.umc.linkyou.domain.enums.AlarmSettingType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alarm_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(nullable = false)
    private boolean alarmAllEnabled;

    @Column(nullable = false)
    private boolean noticeEnabled;

    @Column(nullable = false)
    private boolean linkEnabled;

    @Column(nullable = false)
    private boolean curationEnabled;

    @Column(nullable = false)
    private boolean folderEnabled;

    @Builder(access = AccessLevel.PRIVATE)
    private AlarmSetting(
            Users user,
            boolean alarmAllEnabled,
            boolean noticeEnabled,
            boolean linkEnabled,
            boolean curationEnabled,
            boolean folderEnabled
    ) {
        this.user = user;
        this.alarmAllEnabled = alarmAllEnabled;
        this.noticeEnabled = noticeEnabled;
        this.linkEnabled = linkEnabled;
        this.curationEnabled = curationEnabled;
        this.folderEnabled = folderEnabled;
    }

    public static AlarmSetting createDefault(Users user) {
        return AlarmSetting.builder()
                .user(user)
                .alarmAllEnabled(true)
                .noticeEnabled(true)
                .linkEnabled(true)
                .curationEnabled(true)
                .folderEnabled(true)
                .build();
    }

    // 전체를 off, false 할 때 사용
    public void updateAll(boolean enabled) {
        this.alarmAllEnabled = enabled;
        this.noticeEnabled = enabled;
        this.linkEnabled = enabled;
        this.curationEnabled = enabled;
        this.folderEnabled = enabled;
    }

    public void updateNotice(boolean enabled) {
        this.noticeEnabled = enabled;
    }

    public void updateLink(boolean enabled) {
        this.linkEnabled = enabled;
    }

    public void updateCuration(boolean enabled) {
        this.curationEnabled = enabled;
    }

    public void updateFolder(boolean enabled) {
        this.folderEnabled = enabled;
    }

    public boolean isNoticeActive() {
        return alarmAllEnabled && noticeEnabled;
    }

    public boolean isLinkActive() {
        return alarmAllEnabled && linkEnabled;
    }

    public boolean isCurationActive() {
        return alarmAllEnabled && curationEnabled;
    }

    public boolean isFolderActive() {
        return alarmAllEnabled && folderEnabled;
    }

    public boolean isEnabled(AlarmSettingType alarmSettingType) {
        return switch (alarmSettingType) {
            case ALL -> alarmAllEnabled;
            case NOTICE -> isNoticeActive();
            case LINK -> isLinkActive();
            case CURATION -> isCurationActive();
            case FOLDER -> isFolderActive();
        };
    }
}
