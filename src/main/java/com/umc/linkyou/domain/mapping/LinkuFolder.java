package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.folder.Folder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
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

    @ManyToOne(fet