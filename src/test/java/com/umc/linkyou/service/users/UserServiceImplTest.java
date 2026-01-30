package com.umc.linkyou.service.users;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureRestDocs
class UserServiceImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("비활성 사용자 완전 삭제 스케줄러 테스트")
    void deleteCompletelyInactiveUsers() throws Exception {
        // Given: 10일 지난 INACTIVE 사용자 3명 생성
        Users user1 = createInactiveUser("test1@example.com", LocalDateTime.now().minusDays(11));
        Users user2 = createInactiveUser("test2@example.com", LocalDateTime.now().minusDays(12));
        Users user3 = createInactiveUser("test3@example.com", LocalDateTime.now().minusDays(15));

        // 최근 사용자 (삭제 안됨)
        createInactiveUser("recent@example.com", LocalDateTime.now().minusDays(5));

        em.flush();
        em.clear();

        // When: 스케줄러 실행
        userService.deleteCompletelyInactiveUsers();

        // Then: 3명만 삭제 확인
        long deletedCount = userRepository.countByStatusAndInactiveDateBefore(
                "INACTIVE", LocalDateTime.now().minusDays(10));
        assert deletedCount == 0; // 모두 삭제됨

        // RestDocs 문서화 (로그 확인용)
        mockMvc.perform(RestDocumentationRequestBuilders.get("/admin/scheduler/test")
                        .param("days", "10"))
                .andExpect(status().isOk())
                .andDo(document("delete-inactive-users",
                        PayloadDocumentation.responseFields(
                                PayloadDocumentation.fieldWithPath("deletedCount").description("삭제된 사용자 수"),
                                PayloadDocumentation.fieldWithPath("userIds").description("삭제된 user_id 목록")
                        )));
    }

    private Users createInactiveUser(String email, LocalDateTime inactiveDate) {
        Users user = Users.builder()
                .email(email)
                .nickName(email.split("@")[0])
                .status("INACTIVE")
                .inactiveDate(inactiveDate)
                .build();
        return userRepository.save(user);
    }
}
