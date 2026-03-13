package com.umc.linkyou.domain.mapping.folder;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users_folder")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersFolder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_folder_id")
    private Long userFolderId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false)
    private PermissionType permissionType;

    @Setter
    private Boolean isBookmarked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;
}
