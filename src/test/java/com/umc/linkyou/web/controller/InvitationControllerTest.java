package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.folder.share.InvitationService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("InvitationController 테스트")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final String TOKEN = "invite-token";

    @Nested
    @DisplayName("GET /api/v1/invitations/{token} - 초대장 미리보기")
    class GetInvitationInfo {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("유효한 토큰이면 초대 정보를 반환한다")
            @WithCustomUser(userId = 5L)
            void 유효한토큰_초대정보를_반환한다() throws Exception {
                InvitationInfoResponseDTO response = InvitationInfoResponseDTO.builder()
                        .folderName("어학").ownerName("주인").build();

                given(invitationService.getInvitationInfo(TOKEN)).willReturn(response);

                MvcResult result = mockMvc.perform(get("/api/v1/invitations/{token}", TOKEN))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                InvitationInfoResponseDTO info = readResult(result, objectMapper, InvitationInfoResponseDTO.class);
                assertThat(info.getFolderName()).isEqualTo("어학");
                assertThat(info.getOwnerName()).isEqualTo("주인");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 토큰이면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 5L)
            void 토큰이_없으면_404를_반환한다() throws Exception {
                given(invitationService.getInvitationInfo(TOKEN))
                        .willThrow(new GeneralException(InvitationErrorStatus.INVITATION_NOT_FOUND));

                mockMvc.perform(get("/api/v1/invitations/{token}", TOKEN))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(InvitationErrorStatus.INVITATION_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/invitations/{token}", TOKEN))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/invitations/{token} - 초대 수락")
    class AcceptInvitation {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 참여한 폴더 ID를 반환한다")
            @WithCustomUser(userId = 5L)
            void 정상_요청_시_폴더ID를_반환한다() throws Exception {
                given(invitationService.acceptInvitation(5L, TOKEN)).willReturn(100L);

                mockMvc.perform(post("/api/v1/invitations/{token}", TOKEN).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result").value(100));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("초대 생성자 본인이면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 5L)
            void 생성자본인이면_403을_반환한다() throws Exception {
                given(invitationService.acceptInvitation(5L, TOKEN))
                        .willThrow(new GeneralException(InvitationErrorStatus.INVITATION_CREATOR_CANNOT_ACCEPT));

                mockMvc.perform(post("/api/v1/invitations/{token}", TOKEN).with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(InvitationErrorStatus.INVITATION_CREATOR_CANNOT_ACCEPT.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(post("/api/v1/invitations/{token}", TOKEN).with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
