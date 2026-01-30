package com.umc.linkyou.domain;

import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.*;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = true)
    private Job job;

    @Builder.Default
    @OneToMany(mappedBy ="user", cascade = CascadeType.ALL)
    private List<Purposes> purposes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy ="user", cascade = CascadeType.ALL)
    private List<Interests> interests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE"; // "ACTIVE", "INACTIVE"

    private LocalDateTime inactiveDate;

    @Column(columnDefinition = "TEXT")
    private String deleted_reason;

    //CASCADE
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UsersFolder> usersFoldersList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UsersCategoryColor> usersCategoryColorList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecentViewedLinku> recentViewedLinkus = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuthAccount> authAccounts = new ArrayList<>();

    public void encodePassword(String password) {
        this.password = password;
    }
}
