package com.umc.linkyou.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.converter.TermsConverter;
import com.umc.linkyou.domain.TermsAgreement;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.TermsAgreementRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestExternalConfig.class})
@Transactional
class TermsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TermsAgreementRepository termsAgreementRepository;
    @Autowired private JobRepository jobRepository;

    @Nested
    @DisplayName("회원가입 및 온보딩 약관 통합 테스트")
    class UserRegistrationWithTerms {

        @Test
        @DisplayName("성공 - 일반 회원가입 시 약관 동의 맵이 DB에 정상 반영된다")
        void join_with_terms_success() throws Exception {
            Job job = jobRepository.save(Job.builder()
                    .name("테스트직업")
                    .build());
            UserRequestDTO.JoinDTO request = new UserRequestDTO.JoinDTO(
                    "통합테스터",
                    "integration@test.com",
                    "pass1234",
                    Gender.MALE,
                    job.getId(),
                    List.of("CAREER"),
                    List.of("IT"),
                    Map.of(TermsType.TERMS_OF_USE, true)
            );

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            Users user = userRepository.findByNickName("통합테스터").orElseThrow();
            List<TermsAgreement> agreements = termsAgreementRepository.findAllByUserId(user.getId());

            assertFalse(agreements.isEmpty());
            assertTrue(agreements.stream().anyMatch(a ->
                    a.getTermsType() == TermsType.TERMS_OF_USE && a.getIsAgreed()));
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
                    Map.of(TermsType.MARKETING, true),
                    "v1.0"
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

    @Nested
    @DisplayName("마케팅 약관 동의 토글 테스트")
    class MarketingToggle {

        @Test
        @DisplayName("성공 - 동의(true) 상태에서 토글하면 비동의(false)로 변경된다")
        void toggle_marketing_true_to_false() throws Exception {
            Users user = createUser("토글유저1");
            createMarketingAgreement(user, true);

            mockMvc.perform(patch("/api/v1/users/terms/marketing/toggle")
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("USERS2009"));

            TermsAgreement result = termsAgreementRepository
                    .findByUserIdAndTermsType(user.getId(), TermsType.MARKETING)
                    .orElseThrow();
            assertFalse(result.getIsAgreed());
        }

        @Test
        @DisplayName("성공 - 비동의(false) 상태에서 토글하면 동의(true)로 변경된다")
        void toggle_marketing_false_to_true() throws Exception {
            Users user = createUser("토글유저2");
            createMarketingAgreement(user, false);

            mockMvc.perform(patch("/api/v1/users/terms/marketing/toggle")
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            TermsAgreement result = termsAgreementRepository
                    .findByUserIdAndTermsType(user.getId(), TermsType.MARKETING)
                    .orElseThrow();
            assertTrue(result.getIsAgreed());
        }

        @Test
        @DisplayName("성공 - 마케팅 약관 레코드가 없으면 최초 토글 시 동의(true) 상태로 신규 생성된다")
        void toggle_marketing_creates_record_when_absent() throws Exception {
            Users user = createUser("토글유저3");

            mockMvc.perform(patch("/api/v1/users/terms/marketing/toggle")
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("USERS2009"));

            TermsAgreement result = termsAgreementRepository
                    .findByUserIdAndTermsType(user.getId(), TermsType.MARKETING)
                    .orElseThrow();
            assertTrue(result.getIsAgreed());
            assertFalse(result.getIsRequired());
            assertEquals(TermsConverter.CURRENT_TERMS_VERSION, result.getTermsVersion());
            assertNotNull(result.getAgreedAt());
        }

        @Test
        @DisplayName("성공 - 두 번 토글하면 원래 상태로 돌아온다")
        void toggle_marketing_twice_restores_original_state() throws Exception {
            Users user = createUser("토글유저4");
            createMarketingAgreement(user, true);

            mockMvc.perform(patch("/api/v1/users/terms/marketing/toggle")
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/api/v1/users/terms/marketing/toggle")
                            .with(authentication(authFor(user))))
                    .andExpect(status().isOk());

            TermsAgreement result = termsAgreementRepository
                    .findByUserIdAndTermsType(user.getId(), TermsType.MARKETING)
                    .orElseThrow();
            assertTrue(result.getIsAgreed());
        }
    }

    private void createMarketingAgreement(Users user, boolean isAgreed) {
        termsAgreementRepository.save(TermsAgreement.builder()
                .user(user)
                .termsType(TermsType.MARKETING)
                .isRequired(false)
                .termsVersion(TermsConverter.CURRENT_TERMS_VERSION)
                .agreedAt(LocalDateTime.now())
                .isAgreed(isAgreed)
                .build());
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
