package com.umc.linkyou.service.category;

import com.umc.linkyou.converter.CategoryConverter;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final FcolorRepository fcolorRepository;
    private final UsersCategoryColorRepository usersCategoryColorRepository;
    private final CategoryConverter categoryConverter;

    // 카테고리-기본 컬러 목록 조회
    public List<CategoryListResponseDTO> getCategories(Long userId) {
        // 유저별 카테고리- fcolor 매핑 조회
        List<UsersCategoryColor> usersCategoryColor = usersCategoryColorRepository.findByUserId(userId);

        // 유저별 카테고리 색상
        return usersCategoryColor.stream()
                .map(uc -> {
                    Category category = uc.getCategory();
                    Fcolor fcolor = uc.getFcolor();

                    return CategoryListResponseDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getCategoryName())
                            .colorName(fcolor != null ? fcolor.getColorName() : null)
                            .colorCode1(fcolor != null ? fcolor.getColorCode1() : null)
                            .colorCode2(fcolor != null ? fcolor.getColorCode2() : null)
                            .colorCode3(fcolor != null ? fcolor.getColorCode3() : null)
                            .colorCode4(fcolor != null ? fcolor.getColorCode4() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 유저 카테고리(중분류 폴더) 색상 수정
    @Transactional
    public UserCategoryColorResponseDTO updateUserCategoryColor(
            Long userId,
            Long categoryId,
            UpdateCategoryColorRequestDTO request) {
        UsersCategoryColor ucc = usersCategoryColorRepository.searchCategoryColor(userId, categoryId);

        Fcolor fcolor = fcolorRepository.searchColorCode(request.getFcolorId());
        
        ucc.setFcolor(fcolor);

        return categoryConverter.toUserCategoryColorResponseDTO(ucc);
    }
}
