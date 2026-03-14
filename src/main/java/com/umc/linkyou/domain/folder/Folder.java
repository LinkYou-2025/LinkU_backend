package com.umc.linkyou.domain.folder;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folder")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long folderId;

    @Setter
    @Column(length = 255, nullable = false)
    private String folderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 자기 참조
    // 중분류 폴더이면 NULL, 하위 폴더이면 상위 중분류 폴더 id를 값으로 가진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<LinkuFolder> linkuFolderList = new ArrayList<>();

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<UsersFolder> usersFolderList = new ArrayList<>();
}
