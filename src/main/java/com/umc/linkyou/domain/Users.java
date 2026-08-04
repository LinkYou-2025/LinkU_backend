package com.umc.linkyou.domain;

import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.*;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "users")
public class Users extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "user_id")
    private Long id;


    @NotNull
    @Column(name = "password")
    private String password;

    @Column(name = "nick_name", nullable = false, unique = true)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = true)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "role")
    private Role role = Role.USER;
    // enum 값은 USER로 저장하고, authority 문자열은 "ROLE_USER"로 꺼내 씀

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "inactive_date")
    private LocalDateTime inactiveDate;

    @Column(name = "deleted_reason", columnDefinition = "TEXT")
    private String deleted_reason;

    public void encodePassword(String password) {
        this.password = password;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void updateNickname(String nickName) {
        this.nickName = nickName;
    }

    public void completeSocialProfile(String nickName, Gender gender, Job job) {
        this.nickName = nickName;
        this.gender = gender;
        this.job = job;
        this.status = UserStatus.ACTIVE;
    }

    public void updateProfile(Job job, String nickName) {
        if (job != null) {
            this.job = job;
        }
        if (nickName != null) {
            this.nickName = nickName;
        }
    }

    public void withdraw(String reason, LocalDateTime inactiveDate) {
        this.status = UserStatus.INACTIVE;
        this.inactiveDate = inactiveDate;
        this.deleted_reason = reason;
    }

    public void recover() {
        this.status = UserStatus.ACTIVE;
        this.inactiveDate = null;
        this.deleted_reason = null;
    }
}
