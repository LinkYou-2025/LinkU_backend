package com.umc.linkyou.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.web.dto.UserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@Transactional
class TermsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TermsAgreementRepository termsAgreementRepository;

    @Nested
    @DisplayName("회원가입 및 온보딩 약관 통합 테스트")
    class UserRegistrationWithTerms {

        @Test
        @DisplayName("성공 - 일반 회원가입 시 약관 동의 맵이 DB에 정상 반영된다")
        void join_with_terms_success() throws Exception {
            UserRequestDTO.JoinDTO request = new UserRequestDTO.JoinDTO(
                    "통합테스터",
                    "integration@test.com",
                    "pass1234",
                    1,
                    1L,
                    List.of("GROWTH"),
                    List.of("TECH"),
                    Map.of("TERMS_OF_USE", true, "PRIVACY_POLICY", true)
            );

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            Users user = userRepository.findByNickName("통합테스터").orElseThrow();
            List<TermsAgreement> agreements = termsAgreementRepository.findAllByUserId(user.getId());

            assertEquals(2, agreements.size());
        }
    }

    @Nested
    @DisplayName("로그인 유저 약관 관리 테스트")
    class AuthenticatedUserTerms {

        @Test
        @DisplayName("성공 - 로그인한 사용자가 마케팅 약관을 추가로 동의할 수 있다")
        void update_marketing_terms_success() throws Exception {
            Users user = createUser("로그인유저");

            UserRequestDTO.TermsAgreeDTO request = new UserRequestDTO.TermsAgreeDTO(
                    Map.of("MARKETING", true), "v1.0"
            );

            mockMvc.perform(patch("/api/v1/users/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.termsStatus.MARKETING").value(true));
        }
    }

    private Authentication authFor(Users user) {
        CustomUserDetails principal = new CustomUserDetails(user, "kakao");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private Users createUser(String nickName) {
        return userRepository.save(Users.builder()
                .nickName(nickName)
                .password("DUMMY_PASSWORD")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }
}