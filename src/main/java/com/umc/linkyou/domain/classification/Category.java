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
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fcolor_id", nullable = false)
    private Fcolor fcolor;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Linku> linkuList = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Folder> folderList = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<UsersCategoryColor> usersCategoryColorList = new ArrayList<>();
}
