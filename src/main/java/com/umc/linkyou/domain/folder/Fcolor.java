package com.umc.linkyou.domain.folder;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fcolors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Fcolor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcolor_id")
    private Long fcolorId;

    @Column(name = "color_name", length = 50, nullable = false)
    private String colorName;

    @Column(name = "color_code_1", length = 20, nullable = false)
    private String colorCode1;

    @Column(name = "color_code_2", length = 20, nullable = false)
    private String colorCode2;

    @Column(name = "color_code_3", length = 20, nullable = false)
    private String colorCode3;

    @Column(name = "color_code_4", length = 20, nullable = false)
    private String colorCode4;

    @OneToMany(mappedBy = "fcolor")
    @Builder.Default
    private List<Category> categoryList = new ArrayList<>();

    @OneToMany(mappedBy = "fcolor", cascade = CascadeType.ALL)
    @Builder.Default
    private List<UsersCategoryColor> usersCategoryColorList = new ArrayList<>();
}
