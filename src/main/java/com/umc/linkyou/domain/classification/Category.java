package com.umc.linkyou.domain.classification;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// 폴더 카테고리 (중분류)
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(length = 100, nullable = false)
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fcolor_id", nullable = false)
    private Fcolor fcolor;

    @OneToMany(mappedBy = "category")
    private List<Linku> linkuList = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Folder> folderList = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<UsersCategoryColor> usersCategoryColorList = new ArrayList<>();
}
