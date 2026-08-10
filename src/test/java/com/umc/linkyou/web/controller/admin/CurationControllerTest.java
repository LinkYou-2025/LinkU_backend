package com.umc.linkyou.web.controller.admin;

import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.support.security.MethodSecurityTestConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurationController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, MethodSecurityTestConfig.class})
@DisplayName("어드민 CurationController 테스트")
class CurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurationService curationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long USER_ID = 1L;
    private static final String MONTH = "2026-05";

    @Nested
    @DisplayName("POST /api/v1/admin/curations/batch/manual/user - 단일 유저 큐레이션 생성")
    class TriggerBatchForUser {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("관리자가 유효한 userId와 month를 지정하면 큐레이션을 생성한다")
            @WithCustomUser(userId = 1L, role = Role.ADMIN)
            void 관리자가_요청하면_큐레이션을_생성한다() throws Exception {
                given(curationService.generateCurationForUser(USER_ID, MONTH)).willReturn(true);

                mockMvc.perform(post("/api/v1/admin/curations/batch/manual/user")
                                .param("userId", String.valueOf(USER_ID))
                                .param("month", MONTH)
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.result").isEmpty());

                verify(curationService).generateCurationForUser(USER_ID, MONTH);
            }

            @Test
            @DisplayName("이미 해당 월 큐레이션이 존재해도 200을 반환한다")
            @WithCustomUser(userId = 1L, role = Role.ADMIN)
            void 이미_존재하는_큐레이션이면_생성을_건너뛰고_200을_반환한다() throws Exception {
                given(curationService.generateCurationForUser(USER_ID, MONTH)).willReturn(false);

                mockMvc.perform(post("/api/v1/admin/curations/batch/manual/user")
                                .param("userId", String.valueOf(USER_ID))
                                .param("month", MONTH)
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.result").isEmpty());

                verify(curationService).generateCurationForUser(USER_ID, MONTH);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 유저면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L, role = Role.ADMIN)
            void 존재하지_않는_유저면_예외_응답을_반환한다() throws Exception {
                given(curationService.generateCurationForUser(USER_ID, MONTH))
                        .willThrow(new GeneralException(UserErrorStatus._USER_NOT_FOUND));

                mockMvc.perform(post("/api/v1/admin/curations/batch/manual/user")
                                .param("userId", String.valueOf(USER_ID))
                                .param("month", MONTH)
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(UserErrorStatus._USER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.result").isEmpty());
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(post("/api/v1/admin/curations/batch/manual/user")
                                .param("userId", String.valueOf(USER_ID))
                                .param("month", MONTH)
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("관리자가 아니면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L, role = Role.USER)
            void 관리자가_아니면_403을_반환한다() throws Exception {
                mockMvc.perform(post("/api/v1/admin/curations/batch/manual/user")
                                .param("userId", String.valueOf(USER_ID))
                                .param("month", MONTH)
                                .with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(AuthErrorStatus.PERMISSION_DENIED.getCode()))
                        .andExpect(jsonPath("$.result").isEmpty());

                verifyNoInteractions(curationService);
            }
        }
    }
}
