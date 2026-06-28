package com.umc.linkyou.domain.mapping.folder;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "users_folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UsersFolder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_folder_id")
    private Long userFolderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false)
    private PermissionType permissionType;

    @Column(name = "is_bookmarked")
    @Builder.Default
    private Boolean isBookmarked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = On