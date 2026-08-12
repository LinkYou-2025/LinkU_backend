package com.umc.linkyou.service.category;

import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 단위 테스트")
class CategoryServiceTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FcolorRepository fcolorRepository;

    @Mock
    private UsersCategoryColorRepository usersCategoryColorRepository;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long FCOLOR_ID = 100L;

    private Fcolor fcolor(Long id) {
        return Fcolor.builder()
                .fcolorId(id)
                .colorName("블루")
                .colorCode1("#111111")
                .colorCode2("#222222")
                .colorCode3("#333333")
                .colorCode4("#444444")
                .build();
    }

    @Nested
    @DisplayName("카테고리 목록 조회(getCategories)")
    class GetCategories {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("색상이 지정된 카테고리는 색상 정보를 포함해 반환한다")
            void 색상이_지정된_카테고리는_색상_정보를_포함해_반환한다() {
                Category category = Category.builder().categoryId(CATEGORY_ID).categoryName("어학").build();
                UsersCategoryColor ucc = UsersCategoryColor.builder()
                        .category(category)
                        .fcolor(fcolor(FCOLOR_ID))
                        .build();
                given(usersCategoryColorRepository.findByUserId(USER_ID)).willReturn(List.of(ucc));

                List<CategoryListResponseDTO> result = categoryService.getCategories(USER_ID);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(result.get(0).getCategoryName()).isEqualTo("어학");
                assertThat(result.get(0).getColorName()).isEqualTo("블루");
                assertThat(result.get(0).getColorCode1()).isEqualTo("#111111");
            }

            @Test
            @DisplayName("색상이 지정되지 않은 카테고리는 색상 필드가 null로 반환된다")
            void 색상이_지정되지_않은_카테고리는_색상_필드가_null로_반환된다() {
                Category category = Category.builder().categoryId(CATEGORY_ID).categoryName("업무").build();
                UsersCategoryColor ucc = UsersCategoryColor.builder()
                        .category(category)
                        .fcolor(null)
                        .build();
                given(usersCategoryColorRepository.findByUserId(USER_ID)).willReturn(List.of(ucc));

                List<CategoryListResponseDTO> result = categoryService.getCategories(USER_ID);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getColorName()).isNull();
                assertThat(result.get(0).getColorCode1()).isNull();
            }

            @Test
            @DisplayName("유저가 카테고리를 하나도 안 가지고 있으면 빈 리스트를 반환한다")
            void 유저가_카테고리를_하나도_안_가지고_있으면_빈_리스트를_반환한다() {
                given(usersCategoryColorRepository.findByUserId(USER_ID)).willReturn(List.of());

                List<CategoryListResponseDTO> result = categoryService.getCategories(USER_ID);

                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("유저 카테고리 색상 수정(updateUserCategoryColor)")
    class UpdateUserCategoryColor {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 색상이 변경되고 변경된 정보를 반환한다")
            void 정상_요청_시_색상이_변경되고_변경된_정보를_반환한다() {
                Category category = Category.builder().categoryId(CATEGORY_ID).categoryName("어학").build();
                UsersCategoryColor ucc = UsersCategoryColor.builder()
                        .category(category)
                        .fcolor(fcolor(1L))
                        .build();
                Fcolor newFcolor = fcolor(FCOLOR_ID);

                given(usersCategoryColorRepository.searchCategoryColor(USER_ID, CATEGORY_ID)).willReturn(ucc);
                given(fcolorRepository.searchColorCode(FCOLOR_ID)).willReturn(newFcolor);

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                UserCategoryColorResponseDTO result =
                        categoryService.updateUserCategoryColor(USER_ID, CATEGORY_ID, request);

                assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(result.getFcolorId()).isEqualTo(FCOLOR_ID);
                assertThat(ucc.getFcolor()).isEqualTo(newFcolor);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("해당 유저의 카테고리 색상 정보가 없으면 _CATEGORY_NOT_FOUND를 던진다")
            void 카테고리_색상_정보가_없으면_예외를_던진다() {
                given(usersCategoryColorRepository.searchCategoryColor(USER_ID, CATEGORY_ID)).willReturn(null);

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                assertThatThrownBy(() -> categoryService.updateUserCategoryColor(USER_ID, CATEGORY_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(CategoryErrorStatus._CATEGORY_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 fcolorId면 _FCOLOR_NOT_FOUND를 던진다")
            void fcolorId가_존재하지_않으면_예외를_던진다() {
                Category category = Category.builder().categoryId(CATEGORY_ID).categoryName("어학").build();
                UsersCategoryColor ucc = UsersCategoryColor.builder()
                        .category(category)
                        .fcolor(fcolor(1L))
                        .build();

                given(usersCategoryColorRepository.searchCategoryColor(USER_ID, CATEGORY_ID)).willReturn(ucc);
                given(fcolorRepository.searchColorCode(FCOLOR_ID)).willReturn(null);

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                assertThatThrownBy(() -> categoryService.updateUserCategoryColor(USER_ID, CATEGORY_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(CategoryErrorStatus._FCOLOR_NOT_FOUND));
            }
        }
    }
}
