package com.umc.linkyou.service.users;

import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private JobRepository jobRepository;
    @Mock private CategoryRepository categoryRepository;

    @Test
    @DisplayName("소셜 프로필 완성 시 유저 상태가 TEMP에서 ACTIVE로 변경된다.")
    void socialCompleteProfile_StatusChange() {
        // given
        Users tempUser = Users.builder()
                .id(1L)
                .status(UserStatus.TEMP)
                .usersFoldersList(new ArrayList<>())
                .build();

        UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO();
        request.setNickName("완성닉네임");
        request.setGender(1);
        request.setJobId(1L);
        // 리스트가 null이면 에러가 날 수 있으므로 빈 리스트라도 넣어줍니다.
        request.setPurposeList(new ArrayList<>());
        request.setInterestList(new ArrayList<>());

        when(userRepository.findByNickName(any())).thenReturn(Optional.empty());
        when(jobRepository.findById(any())).thenReturn(Optional.of(Job.builder().id(1L).build()));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());

        // when
        Users result = userService.socialCompleteProfile(tempUser, request);

        // then
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("완성닉네임", result.getNickName());
    }
}
