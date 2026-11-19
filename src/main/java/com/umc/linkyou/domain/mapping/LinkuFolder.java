package com.umc.linkyou.domain.mapping;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.umc.linkyou.domain.folder.Folder;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "linku_folders")
public class LinkuFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linku_folder_id")
    private Long linkuFolderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_linku_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UsersLinku usersLinku;

    public void updateFolder(Folder folder) {
        this.folder = folder;
    }
}
